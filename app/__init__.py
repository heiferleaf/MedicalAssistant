import os

from flask import Flask


def _load_env_file(path: str, *, override: bool = False) -> None:
    """Load KEY=VALUE lines from a .env-like file into os.environ.

    - Best-effort: ignores invalid lines and missing file.
    - No variable expansion.
    """

    if not path:
        return
    if not os.path.exists(path):
        return

    try:
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            for raw in f:
                line = raw.strip()
                if not line or line.startswith("#"):
                    continue
                if "=" not in line:
                    continue
                key, value = line.split("=", 1)
                key = key.strip()
                if key.startswith("export "):
                    key = key[len("export ") :].strip()
                value = value.strip().strip('"').strip("'")
                if not key:
                    continue
                if override or key not in os.environ:
                    os.environ[key] = value
    except Exception:
        return


def _bootstrap_env() -> None:
    # Keep defaults consistent with KG embedding backfill script.
    env_file = (
        os.getenv("FLASK_ENV_FILE")
        or os.getenv("NEO4J_ENV_FILE")
        or os.getenv("MEDICALASSISTANT_ENV_FILE")
        or "/etc/medicalassistant/flask.env"
    )
    # Only fill missing env vars; allow systemd/docker env to take precedence.
    _load_env_file(env_file, override=False)

    # One-click profile switching (only fills missing vars).
    # - AI_PROFILE=local: prefer local Ollama for parsing + answering.
    # - AI_PROFILE=cloud: prefer OpenAI-compatible HTTP API (e.g., DashScope) for parsing + answering.
    # Embedding is intentionally not forced here; keep it aligned with existing KG embeddings.
    profile = (os.getenv("AI_PROFILE") or "local").strip().lower()
    if profile in {"cloud", "api"}:
        os.environ.setdefault("LLM_PROVIDER", "openai")
        os.environ.setdefault("INPUT_PROVIDER", "openai")
        # DashScope OpenAI-compatible mode (Beijing). Users can override per region.
        os.environ.setdefault("LLM_API_BASE", os.getenv("DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"))
        os.environ.setdefault("LLM_MODEL", os.getenv("DASHSCOPE_MODEL", "qwen3.5-plus"))
        os.environ.setdefault("USE_LLM", "true")
    elif profile in {"local", "ollama"}:
        os.environ.setdefault("LLM_PROVIDER", "ollama")
        os.environ.setdefault("INPUT_PROVIDER", "ollama")
        os.environ.setdefault("USE_LLM", "true")


_bootstrap_env()

from app.routes import agent, health, ocr, predict, rag

# 只有在非测试模式下导入agent、predict和ocr模块，避免触发预测模型初始化
import sys
if not any('test_' in arg for arg in sys.argv):
    try:
        from app.routes.agent import agent_bp  # noqa: E402
    except Exception:
        print("Warning: Agent module import failed, skipping agent routes")
    try:
        from app.routes.predict import bp as predict_bp  # noqa: E402
    except Exception:
        print("Warning: Predict module import failed, skipping predict routes")
    try:
        from app.routes.ocr import ocr_bp  # noqa: E402
    except Exception:
        print("Warning: OCR module import failed, skipping OCR routes")
else:
    # 在测试模式下创建一个空的ocr_bp
    from flask import Blueprint
    ocr_bp = Blueprint("ocr", __name__)

def create_app() -> Flask:
    app = Flask(__name__)

    # --- 1. 基础模块 (Health, RAG) ---
    # 注意：这里假设 health.py 和 rag.py 中 Blueprint 实例名仍为 'bp'
    # 如果你也改过它们的名字，请对应修改为 health.health_bp 或 rag.rag_bp
    try:
        app.register_blueprint(health.bp)
        app.register_blueprint(rag.bp, url_prefix="/rag")
    except AttributeError as e:
        print(f"Warning: Failed to register base blueprints (health/rag). Check variable names. {e}")

    # --- 2. OCR 模块 ---
    try:
        from .routes.ocr import ocr_bp
        app.register_blueprint(ocr_bp, url_prefix="/ocr")
    except Exception as e:
        print(f"Warning: OCR module import failed, skipping OCR routes. Error: {e}")

    # --- 3. Predict 模块 ---
    try:
        from .routes.predict import bp as predict_bp
        app.register_blueprint(predict_bp)
    except Exception as e:
        print(f"Warning: Predict module import failed, skipping predict routes. Error: {e}")
        
    # --- 4. Agent 模块 ---
    try:
        from .routes.agent import agent_bp
        app.register_blueprint(agent_bp, url_prefix="/agent")
    except Exception as e:
        print(f"Warning: Agent module import failed, skipping agent routes. Error: {e}")

    # --- 5. Federated 模块 ---
    try:
        from .routes import federated
        app.register_blueprint(federated.bp)
    except Exception as e:
         print(f"Warning: Federated module import failed. Error: {e}")

    return app
