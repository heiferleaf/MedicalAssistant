from flask import Flask

from app.routes.health import health_bp
from app.routes.rag import rag_bp
from app.routes.agent import agent_bp
from app.routes.ocr import ocr_bp


def create_app() -> Flask:
    app = Flask(__name__)

    app.register_blueprint(health_bp)
    app.register_blueprint(rag_bp, url_prefix="/rag")
    app.register_blueprint(agent_bp, url_prefix="/agent")
    app.register_blueprint(ocr_bp, url_prefix="/ocr")

    return app
