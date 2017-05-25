#!/usr/bin/env groovy

def getLibrary() {
	if (env.CHANGE_ID) {
		print "testing library PR ${env.CHANGE_ID}"
		return library("pipeline@refs/remotes/origin/pr/${env.CHANGE_ID}")
	} else {
		print "testing library on master"
		return  library("pipeline")
	}
}

def githubPRCheckout(prId) {
    checkout(
      [
        $class: 'GitSCM', 
        branches: [
          [name: "origin/pr/${prId}"]
        ], 
        doGenerateSubmoduleConfigurations: false, 
        extensions: [], 
        submoduleCfg: [], 
        userRemoteConfigs: [
          [
            credentialsId: 'repo-scan-access', 
            refspec: "+refs/pull/*/head:refs/remotes/origin/pr/*", 
            url: "https://github.com/samsung-cnct/zonar-pipeline-library"
          ]
        ]
      ]
    )
  }


podTemplate(label:"CI-pipeline",containers:[
      getSteps().containerTemplate(name:'jnlp',image:'jenkinsci/jnlp-slave:2.62-alpine',args:'${computer.jnlpmac}${computer.name}'),
      getSteps().containerTemplate(name:'gke',image:"${getSettings().dockerRegistry}/jenkins-gke:latest",ttyEnabled:true,command:'cat',alwaysPullImage:true),
    ]) {
    	node {

			
			stage('integration testing') {
				container('jnlp') {
					def lib = getLibrary()
					
					
				}
			}
			
		}
}

