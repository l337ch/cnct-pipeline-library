#!/usr/bin/env groovy

node {
	print "testing library PR ${env.CHANGE_ID}"
	def lib = library("pipeline@refs/remotes/origin/pr/${env.CHANGE_ID}")
	
	applicationPipeline = lib.net.zonarsystems.pipeline.ApplicationPipeline.new(
	  steps, 
	  'busybox', 
	  this
	)
	applicationPipeline.init()
	applicationPipeline.pipelineRun()
}