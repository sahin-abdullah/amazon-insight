# from flask_smorest import Blueprint, abort
# from flask.views import MethodView
# from amazoncaptcha import AmazonCaptcha
#
# from schemas import UrlSchema, CaptchaSolutionSchema
#
#
# blp = Blueprint(
#     "amazon",
#     import_name=__name__,
#     description="Amazon captcha resolving",
#     url_prefix="/resolve-captcha"
# )
#
#
# @blp.route("/")
# class ResolveCaptcha(MethodView):
#     @blp.arguments(UrlSchema)
#     @blp.response(200, CaptchaSolutionSchema)
#     def post(self, data):
#         image_url = data["image_url"]
#         captcha = AmazonCaptcha.fromlink(image_url)
#         solution = captcha.solve()
#
#         if not solution:
#             abort(400, message="Failed to solve CAPTCHA")
#
#         return {"solution": solution}
