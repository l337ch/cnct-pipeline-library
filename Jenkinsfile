#!/usr/bin/env groovy

node ("CI-library") {
	print "testing library PR ${env.CHANGE_ID}"
	def lib = library("pipeline@refs/remotes/origin/pr/100")
	
	applicationPipeline = lib.net.zonarsystems.pipeline.ApplicationPipeline.new(
	  steps, 
	  'busybox', 
	  this
	)
	applicationPipeline.init()
	applicationPipeline.pipelineRun()
}