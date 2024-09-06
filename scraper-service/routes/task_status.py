from flask.views import MethodView
from flask_smorest import Blueprint
from flask import make_response
from utils import task_statuses

blp = Blueprint(
    "task status endpoint",
    import_name=__name__,
    description="Task Status Endpoint",
    url_prefix="/task-status",
)


@blp.route("/<task_id>")
class TaskStatus(MethodView):
    @blp.response(200)
    def get(self, task_id):
        status = task_statuses.get(task_id, 0)
        response = make_response({"status": status})
        response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, max-age=0'
        response.headers['Pragma'] = 'no-cache'
        response.headers['Expires'] = '0'
        response.headers['Access-Control-Allow-Origin'] = '*'
        return response
