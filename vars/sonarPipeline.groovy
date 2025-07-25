def call(String configFilePath = 'config/prod_config.yaml') {
    def config = readYaml text: libraryResource(configFilePath)

    pipeline {
        agent any

        environment {
            SLACK_CHANNEL = config.SLACK_CHANNEL_NAME
            ENVIRONMENT   = config.ENVIRONMENT
            CODE_BASE     = config.CODE_BASE_PATH
            ACTION_MSG    = config.ACTION_MESSAGE
            KEEP_APPROVAL = config.KEEP_APPROVAL_STAGE
        }

        stages {
            stage('Clone Ansible Repo') {
                steps {
                    git url: 'https://github.com/asmabadrwork/ansible-sonarqube-repo.git'
                }
            }

            stage('User Approval') {
                when {
                    expression { env.KEEP_APPROVAL == 'true' }
                }
                steps {
                    input message: "${env.ACTION_MSG}", ok: 'Proceed'
                }
            }

            stage('Execute Ansible Playbook') {
                steps {
                    sh """
                        ansible-playbook -i SQ_Inventory.ini site.yml
                    """
                }
            }

            stage('Send Slack Notification') {
                steps {
                    slackSend(channel: "#${SLACK_CHANNEL}", message: "SonarQube Deployment Done in ${ENVIRONMENT}")
                }
            }
        }
    }
}

