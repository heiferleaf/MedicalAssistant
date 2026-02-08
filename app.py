import os

from app import create_app


if __name__ == "__main__":
    host = os.getenv("FLASK_HOST", "0.0.0.0")
    port = int(os.getenv("FLASK_PORT", "8001"))
    debug = os.getenv("FLASK_DEBUG", "false").lower() == "true"
    app = create_app()
    app.run(host=host, port=port, debug=debug)
