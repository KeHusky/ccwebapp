set -e
#Usage: setting up our networking resources such as Virtual Private Cloud (VPC), Internet Gateway, Route Table and Routes
# cidr_subnet1=10.0.1.0/24
# cidr_subnet2=10.0.2.0/24
# cidr_subnet3=10.0.3.0/24
# cidr_route=0.0.0.0/0
# STACK_NAME=new

read -p "profile: " PROFILE
export AWS_PROFILE=$PROFILE
if [ -z $PROFILE ]
then 
    echo "cant be null"
    exit
fi

read -p "region: " ZONE
if [ -z $ZONE ]
then
    
    if [ $PROFILE = 'dev' ]
    then 
        ZONE1="us-east-1a"
        ZONE2="us-east-1b"
        ZONE3="us-east-1c"
    else
        ZONE1="us-east-2a"
        ZONE2="us-east-2b"
        ZONE3="us-east-2c"
    fi
else 
    ZONE1=$ZONE"a"
    ZONE2=$ZONE"b"
    ZONE3=$ZONE"c"
fi

read -p "cidr_subnet1: " cidr_subnet1
if [ -z $cidr_subnet1 ]
then cidr_subnet1=10.0.1.0/24
fi

read -p "cidr_subnet2: " cidr_subnet2
if [ -z $cidr_subnet2 ]
then cidr_subnet2=10.0.2.0/24
fi

read -p "cidr_subnet3: " cidr_subnet3
if [ -z $cidr_subnet3 ]
then cidr_subnet3=10.0.3.0/24
fi

read -p "cidr_route: " cidr_route
if [ -z $cidr_route ]
then cidr_route=0.0.0.0/0
fi

read -p "vpc name: " STACK_NAME
if [ -z $STACK_NAME ]
then STACK_NAME=new
fi

#Create VPC and get its Id
vpcId=`aws ec2 create-vpc --cidr-block 10.0.0.0/16 --query 'Vpc.VpcId' --output text`
#Tag vpc
aws ec2 create-tags --resources $vpcId --tags Key=Name,Value=$STACK_NAME
echo "Vpc created-> Vpc Id:  "$vpcId

#Create subnets
subnet1=`aws ec2 create-subnet --vpc-id $vpcId --cidr-block $cidr_subnet1 --availability-zone $ZONE1 --query "Subnet.SubnetId" --output text`
aws ec2 create-tags --resources $subnet1 --tags Key=Name,Value=$STACK_NAME-1
subnet2=`aws ec2 create-subnet --vpc-id $vpcId --cidr-block $cidr_subnet2 --availability-zone $ZONE2 --query "Subnet.SubnetId" --output text`
aws ec2 create-tags --resources $subnet2 --tags Key=Name,Value=$STACK_NAME-2
subnet3=`aws ec2 create-subnet --vpc-id $vpcId --cidr-block $cidr_subnet3 --availability-zone $ZONE3 --query "Subnet.SubnetId" --output text`
aws ec2 create-tags --resources $subnet3 --tags Key=Name,Value=$STACK_NAME-3
echo "3 subnets created"

#Create Internet Gateway
gatewayId=`aws ec2 create-internet-gateway --query 'InternetGateway.InternetGatewayId' --output text`

#Tag Internet Gateway
aws ec2 create-tags --resources $gatewayId --tags Key=Name,Value=$STACK_NAME
echo "Internet gateway created-> gateway Id: "$gatewayId

#Attach Internet Gateway to Vpc
aws ec2 attach-internet-gateway --internet-gateway-id $gatewayId --vpc-id $vpcId
echo "Attached Internet gateway: "$gatewayId" to Vpc: "$vpcId

#Create Route Table
routeTableId=`aws ec2 create-route-table --vpc-id $vpcId --query 'RouteTable.RouteTableId' --output text`
echo "Route table created"

#Tag Route Table
aws ec2 create-tags --resources $routeTableId --tags Key=Name,Value=$STACK_NAME
echo "Route table created -> route table Id: "$routeTableId

#Associate route table with subnets
AssociationId1=`aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $subnet1 --query 'AssociationId' --output text`
echo "Route table " $routeTableId " associated with subnet" $subnet1 ", and association id is:" $AssociationId1
AssociationId2=`aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $subnet2 --query 'AssociationId' --output text`
echo "Route table " $routeTableId " associated with subnet" $subnet2 ", and association id is:" $AssociationId2
AssociationId3=`aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $subnet3 --query 'AssociationId' --output text`
echo "Route table " $routeTableId " associated with subnet" $subnet3 ", and association id is:" $AssociationId3

#Create Route
aws ec2 create-route --route-table-id $routeTableId --destination-cidr-block $cidr_route --gateway-id $gatewayId
echo "Route created: in "$routeTableId" target to "$gatewayId
#Job Done
echo "Job is completed"
