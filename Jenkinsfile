#!/usr/bin/env groovy

File sourceFile = new File("src/net/zonarsystems/pipeline/ApplicationPipeline.groovy");
Class ApplicationPipeline = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile);
ApplicationPipeline applicationPipeline = (GroovyObject) ApplicationPipeline.newInstance(steps, 'pipelinelibrary', this);

//import net.zonarsystems.pipeline.ApplicationPipeline

//applicationPipeline = new ApplicationPipeline(steps, 'pipelinelibrary', this)
applicationPipeline.init()
applicationPipeline.pipelineRun()