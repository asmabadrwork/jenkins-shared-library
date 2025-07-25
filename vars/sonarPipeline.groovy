def call(String configFilePath = 'config/prod_config.yaml') {
    def config = readYaml text: libraryResource(configFilePath)

    pipeline {
        agent any

        stages {
            stage('Initialize Variables') {
                steps {
                    script {
                        env.SLACK_CHANNEL = config.SLACK_CHANNEL_NAME
                        env.ENVIRONMENT   = config.ENVIRONMENT
                        env.CODE_BASE     = config.CODE_BASE_PATH
                        env.ACTION_MSG    = config.ACTION_MESSAGE
                        env.KEEP_APPROVAL = config.KEEP_APPROVAL_STAGE.toString()
                    }
                }
            }

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
                        ansible-playbook -i inventory/hosts.ini main.yml
                    """
                }
            }

            stage('Send Slack Notification') {
                steps {
                    slackSend(channel: "#${env.SLACK_CHANNEL}", message: "SonarQube Deployment Done in ${env.ENVIRONMENT}")
                }
            }
        }
    }
}
