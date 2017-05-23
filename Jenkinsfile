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

node {
	
	dir 'unit_test'
	stage 'unit testing'
		 
	sh './gradlew test'
	junit 'build/test-results/TEST*.xml' 

	
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