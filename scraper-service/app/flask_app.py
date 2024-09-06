from flask import Flask
from flask_smorest import Api
from flask_executor import Executor
from utils import db
from config import DefaultConfig
from routes import scraper_blp, task_status_blp, relevant_reviews_blp


def create_app():
    app = Flask("AmazonInsightApp")
    app.config.from_object(DefaultConfig)

    # Initialize extensions
    api = Api(app)
    db.init_app(app)

    # Register blueprints
    api.register_blueprint(scraper_blp)
    api.register_blueprint(task_status_blp)
    api.register_blueprint(relevant_reviews_blp)

    app.config['EXECUTOR_PUSH_APP_CONTEXT'] = True

    executor = Executor(app)

    with app.app_context():
        # Optional: Reflect the existing database structure (only if needed)
        db.Model.metadata.reflect(db.engine)

    return app
