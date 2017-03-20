#!/usr/bin/env groovy
@Library('pipeline')
import net.zonarsystems.pipeline.ApplicationPipeline

def runTest(applicationPipeline) {
	applicationPipeline = new ApplicationPipeline(
	  steps, 
	  'gprsd', 
	  this
	)
	applicationPipeline.init()
	applicationPipeline.pipelineRun()
}

node {
 	if (env.CHANGE_ID) {
		print "testing library PR ${env.CHANGE_ID}"
		def lib = library("pipeline@refs/remotes/origin/pr/${env.CHANGE_ID}")
		
		applicationPipeline = lib.net.zonarsystems.pipeline.ApplicationPipeline.new(
		  steps, 
		  'pipelinelibrary', 
		  this
		)
		runTest(applicationPipeline)
	} else {
		print "testing library on master"
		applicationPipeline = new ApplicationPipeline(
		  steps, 
		  'pipelinelibrary', 
		  this
		)
		runTest(applicationPipeline)
	}
}