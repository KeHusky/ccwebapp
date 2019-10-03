# CSYE 6225 - Fall 2019

## Team Information

| Name | NEU ID | Email Address |
| --- | --- | --- |
|Ke Yuan |001422663 |xu.yifu@husky.neu.edu |
|Yifu Xu |001491111 |yuan.ke@husky.neu.edu |

## Technology Stack
Postman,Java Spring boot,Mysql workbench.

## Build Instructions
Git clone this repo on your local machine.
Create database for user in mysql.

|Column Name | Datatype |---|
| --- | --- | --- |
|ID |VARCHAR(45) |PK NN UQ |                  
|username|VARCHAR(45)|NN|
|password|VARCHAR(100)|NN|
|firstname|VARCHAR(45)|---|
|lastname|VARCHAR(45)|---|
|account_created|VARCHAR(45)|NN|
|account_updated|VARCHAR(45)|NN|

Add Recipie database

|Column Name | Datatype |---|
| --- | --- | --- |
|ID|VARCHAR(45)|PK NN UQ|
|created_ts|VARCHAR(45)|NN|
|updated_ts|VARCHAR(45)|NN|
|author_id|VARCHAR(45)|NN|
|cook_time_in_min|INT(11)|NN|
|prep_time_in_min|INT(11)|NN|
|total_time_in_min|INT(11)|NN|
|title|VARCHAR(45)|NN|
|cusine|VARCHAR(45)|NN|
|servings|INT(11)|NN|
|ingredients|VARCHAR(100)|NN|
|steps|VARCHAR(100)|NN|
|nutrition_information|VARCHAR(100)|NN|

Run java spring program.

Then open postman to check functions.
## Deploy Instructions


## Running Tests
JUnit

## CI/CD


