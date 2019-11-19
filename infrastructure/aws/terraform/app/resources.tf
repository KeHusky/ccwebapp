#providers
provider "aws" {
        region = "${var.region}"
        access_key = "${var.access_key}"
        secret_key = "${var.secret_key}"
}

#resources

resource "aws_vpc" "vpc" {
  cidr_block = "${var.cidr_vpc}"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags {
    "Environment" = "${var.environment_tag}"
    Name = "${var.vpc_name}"
  }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = "${aws_vpc.vpc.id}"
  tags {
    "Environment" = "${var.environment_tag}"
  }
}

resource "aws_subnet" "subnet_public1" {
  vpc_id = "${aws_vpc.vpc.id}"
  cidr_block = "${var.cidr_subnet1}"
  map_public_ip_on_launch = "true"
  availability_zone = "${var.availability_zone1}"
  tags {
    "Environment" = "${var.environment_tag}"
  }
}

resource "aws_subnet" "subnet_public2" {
  vpc_id = "${aws_vpc.vpc.id}"
  cidr_block = "${var.cidr_subnet2}"
  map_public_ip_on_launch = "true"
  availability_zone = "${var.availability_zone2}"
  tags {
    "Environment" = "${var.environment_tag}"
  }
}

resource "aws_subnet" "subnet_public3" {
  vpc_id = "${aws_vpc.vpc.id}"
  cidr_block = "${var.cidr_subnet3}"
  map_public_ip_on_launch = "true"
  availability_zone = "${var.availability_zone3}"
  tags {
    "Environment" = "${var.environment_tag}"
  }
}

resource "aws_route_table" "rtb_public" {
  vpc_id = "${aws_vpc.vpc.id}"

  route {
      cidr_block = "0.0.0.0/0"
      gateway_id = "${aws_internet_gateway.igw.id}"
  }

  tags {
    "Environment" = "${var.environment_tag}"
  }
}

#resource "aws_route" "r" {
#  route_table_id            = "${aws_route_table.rtb_public.id}"
#  destination_cidr_block    = "0,0,0,0/0"
#  depends_on                = ["aws_route_table.rtb_public"]
#}


resource "aws_route_table_association" "a" {
  subnet_id      = "${aws_subnet.subnet_public1.id}"
  route_table_id = "${aws_route_table.rtb_public.id}"
}

resource "aws_route_table_association" "b" {
  subnet_id      = "${aws_subnet.subnet_public2.id}"
  route_table_id = "${aws_route_table.rtb_public.id}"
}

resource "aws_route_table_association" "c" {
  subnet_id      = "${aws_subnet.subnet_public3.id}"
  route_table_id = "${aws_route_table.rtb_public.id}"
}

#**************************************************************
resource "aws_autoscaling_group" "bar" {
  name                      = "csye6225auto"
  max_size                  = 10
  min_size                  = 3
  health_check_grace_period = 300
  health_check_type         = "EC2"
  force_delete              = true
  launch_template      = {
             id = "${aws_launch_template.foo.id}"
             version = "${aws_launch_template.foo.latest_version}"
}
  target_group_arns  = ["${aws_lb_target_group.test.arn}"]
  vpc_zone_identifier = ["${aws_subnet.subnet_public1.id}","${aws_subnet.subnet_public2.id}","${aws_subnet.subnet_public3.id}"]
  depends_on                = ["aws_db_instance.default"]
}

resource "aws_lb_target_group" "test" {
  name     = "tf-example-lb-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = "${aws_vpc.vpc.id}"
}

resource "aws_launch_template" "foo" {
  name = "foo"

  block_device_mappings {
    device_name = "/dev/sda1"

    ebs {
      volume_size = 20
    }
  }

  #ebs_optimized = true

  iam_instance_profile {
    name = "${aws_iam_instance_profile.instanceprofile.name}"
  }

  image_id ="${var.ami_id}"

  instance_initiated_shutdown_behavior = "terminate"

  instance_type = "t2.micro"

  key_name = "${var.key_pair}"

  monitoring {
    enabled = true
  }

  network_interfaces {
    associate_public_ip_address = true
    security_groups = ["${aws_security_group.app.id}"]
    delete_on_termination = true
  }

  #security_group_names =  ["${aws_security_group.app.name}"]

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "csyewebapp"
    }
  }

  user_data = "${base64encode(data.template_file.user_data.rendered)}"
}

data "aws_sns_topic" "example"{
  name = "csye6225"
}

data "template_file" "user_data"{
   template = "${file("${path.module}/userdata.sh")}"
   vars {
             Endpoint = "${data.aws_db_instance.database.endpoint}"
             bucketName= "${var.aws_bucket}"
             access_key = "${var.access_key}"
             secret_key = "${var.secret_key}"
             region = "${var.region}"
             topic_arn = "${data.aws_sns_topic.example.arn}"
             route53 = "${var.domainName}"
        }
}

output "dbendpoint" {
  value = "${data.aws_db_instance.database.endpoint}"
}

resource "aws_autoscaling_policy" "up" {
  name                   = "up"
  scaling_adjustment     = 1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = "${aws_autoscaling_group.bar.name}"
  #autoscaling_group_name = "csye6225auto"
}

resource "aws_autoscaling_policy" "down" {
  name                   = "down"
  scaling_adjustment     = -1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = "${aws_autoscaling_group.bar.name}"
  #autoscaling_group_name = "csye6225auto"
}

resource "aws_cloudwatch_metric_alarm" "high" {
  alarm_name          = "highalarm"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "300"
  statistic           = "Average"
  threshold           = "5"

  dimensions = {
    #AutoScalingGroupName = "csye6225auto"
    AutoScalingGroupName = "${aws_autoscaling_group.bar.name}"
  }

  alarm_description = "This metric monitors ec2 cpu utilization"
  alarm_actions     = ["${aws_autoscaling_policy.up.arn}"]
}

resource "aws_cloudwatch_metric_alarm" "low" {
  alarm_name          = "lowalarm"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "300"
  statistic           = "Average"
  threshold           = "3"

  dimensions = {
    #AutoScalingGroupName = "csye6225auto"
    AutoScalingGroupName = "${aws_autoscaling_group.bar.name}"
  }

  alarm_description = "This metric monitors ec2 cpu utilization"
  alarm_actions     = ["${aws_autoscaling_policy.down.arn}"]
}


resource "aws_lb" "csyelb" {
  name               = "csye6225loadB"
  internal           = false
  load_balancer_type = "application"
  security_groups    = ["${aws_security_group.lb_sg.id}"]
  subnets            = ["${aws_subnet.subnet_public1.id}","${aws_subnet.subnet_public2.id}","${aws_subnet.subnet_public3.id}"]

  enable_deletion_protection = true

  tags = {
    Environment = "production"
	Name = "CSYE6225"
  }
}

resource "aws_lb_listener" "front_end" {
  load_balancer_arn = "${aws_lb.csyelb.arn}"
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = "${var.certificate_arn}"

  default_action {
    type             = "forward"
    target_group_arn = "${aws_lb_target_group.test.arn}"
  }
}

resource "aws_lb_listener_certificate" "example" {
  listener_arn    = "${aws_lb_listener.front_end.arn}"
  certificate_arn = "${var.certificate_arn}"
}

#resource "aws_acm_certificate" "cert" {
#  domain_name       = "${var.domainName}"
#  validation_method = "DNS"
#  tags = {
#    Environment = "test"
#  }
#  lifecycle {
#    create_before_destroy = true
#  }
#}

data "aws_route53_zone" "example" {
  name = "${var.domainName}"
}

resource "aws_route53_record" "www" {
  zone_id = "${data.aws_route53_zone.example.zone_id}"
  name    = "${var.domainName}"
  type    = "A"

  alias {
    name                   = "${aws_lb.csyelb.dns_name}"
    zone_id                = "${aws_lb.csyelb.zone_id}"
    evaluate_target_health = true
  }
}

resource "aws_security_group" "lb_sg" {
  name        = "lb"
  vpc_id      = "${aws_vpc.vpc.id}"

  ingress {
    # TLS (change to whatever ports you need)
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
 
   ingress {
    # TLS (change to whatever ports you need)
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
 
  egress{
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
	security_groups = ["${aws_security_group.app.id}"]
  }

}

data "aws_db_instance" "database"{
 db_instance_identifier = "csye6225-fall2019"
 depends_on = ["aws_db_instance.default"]
}

resource "aws_iam_instance_profile" "instanceprofile" {
  name = "profile"
  role = "CodeDeployEC2ServiceRole"
}


#resource "aws_volume_attachment" "ebs_att" {
#  device_name = "/dev/sdh"
#  volume_id   = "${aws_ebs_volume.example.id}"
#  instance_id = "${aws_instance.foo.id}"
#  force_detach = true
#}

#resource "aws_ebs_volume" "example" {
#  availability_zone = "${var.availability_zone1}"
#  size              = 1
#}

resource "aws_security_group" "app" {
  name        = "app"
  vpc_id      = "${aws_vpc.vpc.id}"

  ingress {
    # TLS (change to whatever ports you need)
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    # TLS (change to whatever ports you need)
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
   ingress {
    # TLS (change to whatever ports you need)
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

   ingress {
    # TLS (change to whatever ports you need)
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]

  }

  egress{
    from_port  = 0
    to_port    = 0
    protocol   = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

}

resource "aws_security_group" "DB" {
  name        = "DB"
  vpc_id      = "${aws_vpc.vpc.id}"
  
  ingress {
    # TLS (change to whatever ports you need)
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    security_groups = ["${aws_security_group.app.id}"]
   
  }

  ingress {
    # TLS (change to whatever ports you need)
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    security_groups = ["${aws_security_group.app.id}"]
  } 

  egress{
    from_port  = 0
    to_port    = 0
    protocol   = "-1"
  }
}


resource "aws_db_subnet_group" "default" {
  name = "main"
  subnet_ids = ["${aws_subnet.subnet_public1.id}","${aws_subnet.subnet_public2.id}"]
}

resource "aws_db_instance" "default" {
  allocated_storage    = 20
  storage_type         = "gp2"
  engine               = "mysql"
  engine_version       = "5.7"
  instance_class       = "db.t2.micro"
  name                 = "csye6225"
  username             = "dbuser"
  password             = "abc123456"
  identifier           = "csye6225-fall2019"
  db_subnet_group_name = "${aws_db_subnet_group.default.id}"
  vpc_security_group_ids = ["${aws_security_group.DB.id}"]
  skip_final_snapshot = true
}

resource "aws_dynamodb_table" "basic-dynamodb-table" {
  name           = "csye6225"
  hash_key       = "id"
  billing_mode = "PROVISIONED"
  read_capacity = 20
  write_capacity = 20
  attribute {
    name = "id"
    type = "S"
  }
  ttl{
  attribute_name = "TimeToExist"
  enabled = true
}
  tags = {
    Name        = "dynamodb-table-1"
    Environment = "production"
  }
}

data "aws_iam_role" "example"{
  name = "CodeDeployServiceRole"
}

resource "aws_codedeploy_app" "example" {
  compute_platform = "Server"
  name             = "csye6225-webapp"
}

resource "aws_codedeploy_deployment_group" "foo" {
  app_name               = "${aws_codedeploy_app.example.name}"
  deployment_group_name  = "csye6225-webapp-deployment"
  service_role_arn       = "${data.aws_iam_role.example.arn}"
  deployment_config_name = "CodeDeployDefault.AllAtOnce"
  #autoscaling_groups = ["csye6225auto"]
  autoscaling_groups = ["${aws_autoscaling_group.bar.name}"]
  load_balancer_info {
                         target_group_info {
                                               name="${aws_lb_target_group.test.name}"
                                           }                           
                    } 

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

