#!/usr/bin/groovy
def stage(){
  return stageProject{
    project = 'fabric8-apps/exposecontroller-app'
    useGitTagForNextVersion = true
  }
}

def approveRelease(project){
  def releaseVersion = project[1]
  approve{
    room = null
    version = releaseVersion
    console = null
    environment = 'fabric8'
  }
}

def release(project){
  releaseProject{
    stagedProject = project
    useGitTagForNextVersion = true
    helmPush = false
    groupId = 'io.fabric8.apps'
    githubOrganisation = 'fabric8-apps'
    artifactIdToWatchInCentral = 'exposecontroller-app'
    artifactExtensionToWatchInCentral = 'jar'
    promoteToDockerRegistry = 'docker.io'
    dockerOrganisation = 'fabric8'
    imagesToPromoteToDockerHub = []
    extraImagesToTag = null
  }
}

def updateDownstreamDependencies(stagedProject) {
  pushPomPropertyChangePR {
    propertyName = 'exposecontroller.version'
    projects = [
            'fabric8io/fabric8-platform',
            'fabric8-services/fabric8-tenant-jenkins'
    ]
    version = stagedProject[1]
  }
  updateInitService(stagedProject[1])
}

def updateInitService(releaseVersion){
  ws{
    container(name: 'clients') {
      def flow = new io.fabric8.Fabric8Commands()
      sh 'chmod 600 /root/.ssh-git/ssh-key'
      sh 'chmod 600 /root/.ssh-git/ssh-key.pub'
      sh 'chmod 700 /root/.ssh-git'

      git 'git@github.com:fabric8-services/fabric8-tenant.git'

      sh "git config user.email fabric8cd@gmail.com"
      sh "git config user.name fabric8-cd"

      def uid = UUID.randomUUID().toString()
      sh "git checkout -b versionUpdate${uid}"

      sh "echo ${releaseVersion} > EXPOSCONTROLLER_VERSION"
      def message = "Update fabric8-platform version to ${releaseVersion}"
      sh "git commit -a -m \"${message}\""
      sh "git push origin versionUpdate${uid}"

      def prId = flow.createPullRequest(message,'fabric8-services/fabric8-tenant',"versionUpdate${uid}")
      flow.mergePR('fabric8-services/fabric8-tenant',prId)
    }
  }
}
return this;
