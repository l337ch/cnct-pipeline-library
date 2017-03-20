#!/usr/bin/env groovy

node ("CI-library") {
	print "testing library PR ${env.CHANGE_ID}"
	
	import library("pipeline@refs/remotes/origin/pr/100").net.zonarsystems.pipeline.ApplicationPipeline
	
	applicationPipeline = new ApplicationPipeline(
	  steps, 
	  'busybox', 
	  this
	)
	applicationPipeline.init()
	applicationPipeline.pipelineRun()
}