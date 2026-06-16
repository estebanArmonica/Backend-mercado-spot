pipeline {
    agent any
    
    environment {
        // Variables desde Jenkins (no las pongas en el código)
        DOCKER_IMAGE = 'tu-usuario/etl-backend-mercado-spot'
        PROJECT_ID = 'tu-proyecto-gcp'
        SERVICE_NAME = 'etl-backend-mercado-spot'
        REGION = 'us-central1'
    }
    
    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/tu-usuario/Backend-ETL-Mercado-Spot.git',
                    branch: 'main',
                    credentialsId: 'estebanArmonica'
            }
        }
        
        stage('Build with Maven') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test',
                //junit 'target/surefire-reports/*.xml'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} -f Dockerfile.prod ."
                    sh "docker tag ${DOCKER_IMAGE}:${BUILD_NUMBER} ${DOCKER_IMAGE}:latest"
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-hub',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}
                        docker push ${DOCKER_IMAGE}:latest
                    '''
                }
            }
        }
        
        stage('Deploy to Cloud Run') {
            steps {
                withCredentials([file(credentialsId: 'gcp-key', variable: 'GCP_KEY')]) {
                    sh '''
                        gcloud auth activate-service-account --key-file=$GCP_KEY
                        gcloud config set project ${PROJECT_ID}
                        gcloud run deploy ${SERVICE_NAME} \
                            --image ${DOCKER_IMAGE}:${BUILD_NUMBER} \
                            --region ${REGION} \
                            --platform managed \
                            --allow-unauthenticated \
                            --memory 1Gi \
                            --timeout 3600 \
                            --set-env-vars "SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL},SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME},SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD},JWT_SECRET=${JWT_SECRET},JWT_EXPIRATION=${JWT_EXPIRATION}"
                    '''
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ Pipeline ejecutado exitosamente!'
        }
        failure {
            echo '❌ Pipeline falló. Revisa los logs.'
        }
    }
}