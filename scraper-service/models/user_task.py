from datetime import datetime
from utils import db


class UserTask(db.Model):
    __tablename__ = 'user_tasks'

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), nullable=False)
    task_id = db.Column(db.String(36), nullable=False)
    start_date = db.Column(db.DateTime, nullable=False, default=datetime.utcnow)
    end_date = db.Column(db.DateTime)

    def __repr__(self):
        return f'<UserTask {self.username} - {self.task_id}>'
