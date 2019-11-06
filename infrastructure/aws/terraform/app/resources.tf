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
resource "aws_instance" "foo" {
  ami           = "${var.ami_id}"
  key_name = "${var.key_pair}"
  instance_type = "t2.micro"
  iam_instance_profile = "${aws_iam_instance_profile.instanceprofile.name}"
  user_data = "${data.template_file.user_data.rendered}"
   root_block_device{ 
   volume_size = 20
   volume_type = "gp2"
  }
  vpc_security_group_ids = ["${aws_security_group.app.id}"]
  subnet_id  = "${aws_subnet.subnet_public1.id}"
  tags {   
    Name = "csyewebapp"
   }
  depends_on                = ["aws_db_instance.default"]
}


data "template_file" "user_data"{
   template = "${file("${path.module}/userdata.sh")}"
   vars {
             Endpoint = "${data.aws_db_instance.database.endpoint}"
             bucketName= "${var.aws_bucket}"
        }
}

output "dbendpoint" {
  value = "${data.aws_db_instance.database.endpoint}"
}

data "aws_db_instance" "database"{
 db_instance_identifier = "csye6225-fall2019"
 depends_on = ["aws_db_instance.default"]
}

resource "aws_iam_instance_profile" "instanceprofile" {
  name = "profile"
  role = "CodeDeployServiceRole"
}


resource "aws_volume_attachment" "ebs_att" {
  device_name = "/dev/sdh"
  volume_id   = "${aws_ebs_volume.example.id}"
  instance_id = "${aws_instance.foo.id}"
  force_detach = true
}

resource "aws_ebs_volume" "example" {
  availability_zone = "${var.availability_zone1}"
  size              = 1
}

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

resource "aws_s3_bucket" "bucket" {

  
  bucket = "${var.aws_bucket}"

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
      days = 90
    }
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

  tags = {
    Name        = "dynamodb-table-1"
    Environment = "production"
  }
}


