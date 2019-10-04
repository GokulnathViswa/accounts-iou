# accounts-iou
* This is project to create IOU between two accounts.
* Run the command in the project directory './gradlew clean deployNodes'
* Start all the node.
* Start the client webserver
* Create an account in PartyA and PartyB using the api 'createAccount' in the Controller.kt
* Share PartyA's account to PartyB using the api 'ShareAccountInfoFlow' in the Controller.kt
* Create IOU from PartyB as a lender using the api 'createIOU' in the Controller.kt
* Check the AccountsIOUState data using the api 'getIOUStates' in the Controller.kt

Note:
Accept IOU is not working properly. It is throwing error in the FinalityFlow.
