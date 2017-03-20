#!/usr/bin/env groovy

node ("CI-library") {
	print "testing library PR ${env.CHANGE_ID}"
	def lib = library("pipeline@refs/remotes/origin/pr/100")
	import .net.zonarsystems.pipeline.ApplicationPipeline
	
	applicationPipeline = new ApplicationPipeline(
	  steps, 
	  'busybox', 
	  this
	)
	applicationPipeline.init()
	applicationPipeline.pipelineRun()
}