provider "aws" {
        region = "${var.AWS_REGION}"
        access_key = "${var.access_key}"
        secret_key = "${var.secret_key}"
}

resource "aws_iam_policy" "DeployEC2S3" {
  name        = "CodeDeploy-EC2-S3"
  path        = "/"
  description = "CodeDeploy-EC2-S3"

  policy = <<EOF
{
   "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "s3:Get*",
                "s3:List*"
            ],
            "Effect": "Allow",
            "Resource": "${aws_iam_policy.AttachmentToS3Bucket.arn}"
        }
    ]
}
EOF
}

resource "aws_iam_user_policy" "UploadToS3" {
  name        = "CircleCI-Upload-To-S3"
  user = "${var.cicd_name}"
  #description = "CircleCI-Upload-To-S3"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject"
            ],
            "Resource": [
                "*"
            ]
        }
    ]
}
EOF
}

locals{
AWS_REGION = "${var.AWS_REGION}"
AWS_ACCOUNT_ID ="${var.AWS_ACCOUNT_ID}"
}

resource "aws_iam_user_policy" "CircleCICodeDeploy" {
  name        = "CircleCI-Code-Deploy"
  user = "${var.cicd_name}"
 #description = "CircleCI-Code-Deploy"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "codedeploy:RegisterApplicationRevision",
        "codedeploy:GetApplicationRevision"
      ],
      "Resource": [
        
          "arn:aws:codedeploy:us-east-1:494805991375:application:csye6225-webapp"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "codedeploy:CreateDeployment",
        "codedeploy:GetDeployment"
      ],
      "Resource": [
        "*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "codedeploy:GetDeploymentConfig"
      ],
      "Resource": [
        "arn:aws:codedeploy:us-east-1:494805991375:deploymentconfig:CodeDeployDefault.OneAtATime",
        "arn:aws:codedeploy:us-east-1:494805991375:deploymentconfig:CodeDeployDefault.HalfAtATime",
        "arn:aws:codedeploy:us-east-1:494805991375:deploymentconfig:CodeDeployDefault.AllAtOnce"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_user_policy" "circleciec2ami" {
  name        = "circleci-ec2-ami"
  user = "${var.cicd_name}"
  #description = "circleci-ec2-ami"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
      "Effect": "Allow",
      "Action" : [
        "ec2:AttachVolume",
        "ec2:AuthorizeSecurityGroupIngress",
        "ec2:CopyImage",
        "ec2:CreateImage",
        "ec2:CreateKeypair",
        "ec2:CreateSecurityGroup",
        "ec2:CreateSnapshot",
        "ec2:CreateTags",
        "ec2:CreateVolume",
        "ec2:DeleteKeyPair",
        "ec2:DeleteSecurityGroup",
        "ec2:DeleteSnapshot",
        "ec2:DeleteVolume",
        "ec2:DeregisterImage",
        "ec2:DescribeImageAttribute",
        "ec2:DescribeImages",
        "ec2:DescribeInstances",
        "ec2:DescribeInstanceStatus",
        "ec2:DescribeRegions",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeSnapshots",
        "ec2:DescribeSubnets",
        "ec2:DescribeTags",
        "ec2:DescribeVolumes",
        "ec2:DetachVolume",
        "ec2:GetPasswordData",
        "ec2:ModifyImageAttribute",
        "ec2:ModifyInstanceAttribute",
        "ec2:ModifySnapshotAttribute",
        "ec2:RegisterImage",
        "ec2:RunInstances",
        "ec2:StopInstances",
        "ec2:TerminateInstances"
      ],
      "Resource" : "*"
  }]
}
EOF
}

resource "aws_iam_policy" "AttachmentToS3Bucket" {
  name        = "AccessAttachmentToS3Bucket"
  description = "AccessAttachmentToS3Bucket"

  policy = <<EOF
{
     "Version" : "2012-10-17",
      "Statement":[
                     { "Action":[
                                  "s3:Get*",
                                  "s3:List*",
                                  "s3:Delete*",
                                  "s3:Put*"
                                 ],
                        "Effect":"Allow",
                        "Resource" : [
                                         "${aws_s3_bucket.deploybucket.arn}"                            
                                     ]
                     }
                  ]         
}
EOF
}


#Role

resource "aws_iam_role" "EC2ServiceRole" {
  name = "CodeDeployEC2ServiceRole"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF

  tags = {
    tag-key = "tag-value"
  }
}

resource "aws_iam_role_policy_attachment" "attach1" {
  role       = "${aws_iam_role.EC2ServiceRole.name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonSNSFullAccess"
 }

resource "aws_iam_role_policy_attachment" "attach3" {
  role       = "${aws_iam_role.EC2ServiceRole.name}"
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
 }

resource "aws_iam_role_policy_attachment" "attach4" {
  role       = "${aws_iam_role.EC2ServiceRole.name}"
  policy_arn = "${aws_iam_policy.DeployEC2S3.arn}"
 }

resource "aws_iam_role" "DeployServiceRole" {
  name = "CodeDeployServiceRole"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "codedeploy.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF

  tags = {
    tag-key = "tag-value"
  }
}

resource "aws_iam_role_policy_attachment" "attach2" {
  role       = "${aws_iam_role.DeployServiceRole.name}"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSCodeDeployRole"
               
}


resource "aws_s3_bucket" "deploybucket" {

  
  bucket = "${var.deploy_bucket}"

  force_destroy = true
  
  server_side_encryption_configuration{
  rule{
     apply_server_side_encryption_by_default{

         sse_algorithm = "aws:kms"
     }
  }
}

  acl    = "private"

  lifecycle_rule {
    id      = "log"
    enabled = true

    prefix = "log/"

    tags = {
      "rule"      = "log"
      "autoclean" = "true"
    }

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    expiration {
      days = 60
    }
  }
}



resource "aws_codedeploy_app" "example" {
  compute_platform = "Server"
  name             = "csye6225-webapp"
}

resource "aws_codedeploy_deployment_group" "foo" {
  app_name               = "${aws_codedeploy_app.example.name}"
  deployment_group_name  = "csye6225-webapp-deployment"
  service_role_arn       = "${aws_iam_role.DeployServiceRole.arn}"
  deployment_config_name = "CodeDeployDefault.AllAtOnce"

  ec2_tag_filter {
    key   = "Name"
    type  = "KEY_AND_VALUE"
    value = "csyewebapp"
  }

  auto_rollback_configuration {
    enabled = true
    events  = ["DEPLOYMENT_FAILURE"]
  }

}
