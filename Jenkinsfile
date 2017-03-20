#!/usr/bin/env groovy
print "testing library PR ${env.JOB_NAME}"
@Library("pipeline@refs/remotes/origin/pr/${env.JOB_NAME}")
import net.zonarsystems.pipeline.ApplicationPipeline

applicationPipeline = new ApplicationPipeline(
  steps, 
  'busybox', 
  this
)
applicationPipeline.init()
applicationPipeline.pipelineRun()