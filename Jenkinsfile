#!/usr/bin/env groovy

node ("CI-library") {
	print "testing library PR ${env.CHANGE_ID}"
	def lib = library("pipeline@refs/remotes/origin/pr/100")
	
	applicationPipeline = new net.zonarsystems.pipeline.ApplicationPipeline(
	  steps, 
	  'busybox', 
	  this
	)
	applicationPipeline.init()
	applicationPipeline.pipelineRun()
}