provider "aws" {
        region = "${var.region}"
        access_key = "${var.access_key}"
        secret_key = "${var.secret_key}"
}

resource "aws_lambda_permission" "with_sns" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.func.function_name}"
  principal     = "sns.amazonaws.com"
  source_arn    = "${aws_sns_topic.default.arn}"
}

resource "aws_iam_role_policy_attachment" "attach1" {
  role       = "${aws_iam_role.iam_for_lambda.name}"
  policy_arn =   "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "attach2" {
  role       = "${aws_iam_role.iam_for_lambda.name}"
  policy_arn =  "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
}

resource "aws_iam_role_policy_attachment" "attach3" {
  role       = "${aws_iam_role.iam_for_lambda.name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonRoute53FullAccess"
}                   
                    
resource "aws_iam_role_policy_attachment" "attach4" {
  role       = "${aws_iam_role.iam_for_lambda.name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonSESFullAccess",
}                   

resource "aws_iam_role_policy_attachment" "attach5" {
  role       = "${aws_iam_role.iam_for_lambda.name}"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaDynamoDBExecutionRole"
}

resource "aws_iam_role_policy_attachment" "attach6" {
  role       = "${aws_iam_role.iam_for_lambda.name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonSNSFullAccess"
}

resource "aws_iam_role_policy_attachment" "attach7" {
  role       = "${aws_iam_role.iam_for_lambda.name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}


resource "aws_iam_role" "iam_for_lambda" {
  name = "iam_for_lambda"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_sns_topic" "default" {
  name = "csye6225"
}

resource "aws_sns_topic_subscription" "lambda" {
  topic_arn = "${aws_sns_topic.default.arn}"
  protocol  = "lambda"
  endpoint  = "${aws_lambda_function.func.arn}"
}

resource "aws_lambda_function" "func" {
  #filename      = "lambda-0.0.1.zip"
  s3_bucket     = "${var.codedeploybucket}"
  s3_key = "lambda-0.0.1.zip"
  function_name = "demo"
  role          = "${aws_iam_role.iam_for_lambda.arn}"
  handler       = "LogEvent::handleRequest"
  runtime       = "java8"

  depends_on    = ["aws_iam_role_policy_attachment.lambda_logs"]
}

# See also the following AWS managed policy: AWSLambdaBasicExecutionRole
resource "aws_iam_policy" "lambda_logging" {
  name = "lambda_logging"
  path = "/"
  description = "IAM policy for logging from a lambda"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*",
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role = "${aws_iam_role.iam_for_lambda.name}"
  policy_arn = "${aws_iam_policy.lambda_logging.arn}"
}
