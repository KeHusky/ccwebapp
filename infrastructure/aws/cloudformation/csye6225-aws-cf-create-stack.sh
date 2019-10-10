read -p "profile: " PROFILE
export AWS_PROFILE=$PROFILE
if [ -z $PROFILE ]
then 
    echo "cant be null"
    exit
fi
read -p "mVPCSubnetCidrBlock: " mVPCSubnetCidrBlock
if [ -z $mVPCSubnetCidrBlock ]
then mVPCSubnetCidrBlock=10.0.0.0/16
fi
read -p "mAvailabilityZone1: " mAvailabilityZone1
if [ -z $mAvailabilityZone1 ]
then mAvailabilityZone1=a
fi
read -p "mAvailabilityZone2: " mAvailabilityZone2
if [ -z $mAvailabilityZone2 ]
then mAvailabilityZone2=b
fi
read -p "mAvailabilityZone3: " mAvailabilityZone3
if [ -z $mAvailabilityZone3 ]
then mAvailabilityZone3=c
fi
read -p "mSubnetCidrBlock1: " mSubnetCidrBlock1
if [ -z $mSubnetCidrBlock1 ]
then mSubnetCidrBlock1=10.0.1.0/24
fi
read -p "mSubnetCidrBlock2: " mSubnetCidrBlock2
if [ -z $mSubnetCidrBlock2 ]
then mSubnetCidrBlock2=10.0.2.0/24
fi
read -p "mSubnetCidrBlock3: " mSubnetCidrBlock3
if [ -z $mSubnetCidrBlock3 ]
then mSubnetCidrBlock3=10.0.3.0/24
fi
read -p "mRouteIpaddress: " mRouteIpaddress
if [ -z $mRouteIpaddress ]
then mRouteIpaddress=0.0.0.0/0
fi

read -p "stack name: " sName
if [ -z $sName ]
then 
    echo "cant be null"
    exit
fi
# mVPCSubnetCidrBlock=10.0.0.0/16
# mAvailabilityZone1=a
# mAvailabilityZone2=b
# mAvailabilityZone3=c
# mSubnetCidrBlock1=10.0.1.0/24
# mSubnetCidrBlock2=10.0.2.0/24
# mSubnetCidrBlock3=10.0.3.0/24
# mRouteIpaddress=0.0.0.0/0
	StackName=$sName

	echo "$sName Stack creation in progress..."

	stackID=$(aws cloudformation create-stack --stack-name $sName --template-body file://csye6225-cf-networking.json --parameters ParameterKey=VPCSubnetCidrBlock,ParameterValue=$mVPCSubnetCidrBlock ParameterKey=AvailabilityZone1,ParameterValue=$mAvailabilityZone1 ParameterKey=AvailabilityZone2,ParameterValue=$mAvailabilityZone2 ParameterKey=AvailabilityZone3,ParameterValue=$mAvailabilityZone3  ParameterKey=PublicSubnetCidrBlock1,ParameterValue=$mSubnetCidrBlock1 ParameterKey=PublicSubnetCidrBlock2,ParameterValue=$mSubnetCidrBlock2 ParameterKey=PublicSubnetCidrBlock3,ParameterValue=$mSubnetCidrBlock3 ParameterKey=RouteIpaddress,ParameterValue=$mRouteIpaddress ParameterKey=StackName,ParameterValue=$StackName --query [StackId] --output text)
	aws cloudformation wait stack-create-complete --stack-name $stackID

	echo $stackID

	if [ -z $stackID ]; then
		echo 'Error. Stack creation failed !!!'
		exit 1
	else
		echo "Stack Creation Done !!!"
fi
