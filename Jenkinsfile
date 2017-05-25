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

node {

	//stage 'checkout'
	githubPRCheckout(env.CHANGE_ID)
	stage 'unit testing'
	
	dir ('./unit_test') {
		sh 'pwd'
		sh 'ls'
		sh './gradlew test --debug'
		junit 'build/test-results/TEST*.xml' 
	}
	stage 'integration testing'

	def lib = getLibrary()
	
	applicationPipeline = lib.net.zonarsystems.pipeline.ApplicationPipeline.new(
	  steps, 
	  'pipelinelibrary', 
	  this
	)
	applicationPipeline.init()
	applicationPipeline.pipelineRun()

}