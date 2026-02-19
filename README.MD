 Project work flow    

     No role based login 
     create tender 
     bid into the tender 
      upload docs
      apply ocr if image is directly converted into the pdf
      after that apply the rule based key valiadtion for doc verifation
       show  duplicate data and missing doc 
       mannual review  
         result converted into the Zip files
         zip file upload into the pinata ifsc  and genrate a key id 
          key id is store into the blockchain   cardano testnet







TO run the Frontend 
 Into the terminal write the command cd frontend 
                                      ng serve

  to run the backend
    wirte the command --- > cd backend 

                           mvn spring-boot:run


Api testing for the Tender 

  /api/tenders/all     Get
  /api/temder/5        Get
  /api/tender/create



  /api/tenders/create	POST		Tender created (ID: 6, budget: 75000)
/api/tenders/all	GET		Returns all 6 tenders
/api/tenders/5	GET		Returns single tender with budget
/api/tenders/user/1	GET		Returns user-specific tenders 



  

 For the OCR 

    Google Documnet Ai   
      Project id  = tender-487906   

      processsor id=582a9f90ef4a0f0a