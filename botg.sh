#!/bin/bash
reset
javaPrefix="java -Xmx14g -cp target/botg.jar"
set -e
set -x

mvn clean package

dataset='ohsumed'
foldsDirname='foldDistributions_allSamples'

#$javaPrefix botg.baseline.graph.GraphGenerator dataset=${dataset},foldsDirname=${foldsDirname},usePriorDatasetCrossFoldDistribution=true,graphs=graphs-withTitle-tfIdf-minDfTerm2,useTfIdf=true,minimumTermDF=2
#$javaPrefix botg.baseline.graph.GraphGenerator dataset=${dataset},foldsDirname=${foldsDirname},usePriorDatasetCrossFoldDistribution=true,graphs=graphs-withTitle-tfIdf-minDfTerm2-reach3,forcedReach=3,useTfIdf=true,minimumTermDF=2
#$javaPrefix botg.baseline.graph.GraphGenerator dataset=${dataset},foldsDirname=${foldsDirname},usePriorDatasetCrossFoldDistribution=true,graphs=graphs-withTitle-tfIdf-minDfTerm2-reach5,forcedReach=5,useTfIdf=true,minimumTermDF=2
#$javaPrefix botg.baseline.graph.GraphGenerator dataset=${dataset},foldsDirname=${foldsDirname},usePriorDatasetCrossFoldDistribution=true,graphs=graphs-withTitle-tf,useTfIdf=false,minimumTermDF=0
#$javaPrefix botg.baseline.graph.GraphGenerator dataset=${dataset},foldsDirname=${foldsDirname},usePriorDatasetCrossFoldDistribution=true,graphs=graphs-withTitle-tfIdf,useTfIdf=true,minimumTermDF=0

$javaPrefix botg.BoTG dataset=$dataset,foldsDirname=$foldsDirname,graphs=graphs-withTitle-tfIdf-minDfTerm2,filterCodebookSet=false,randomCodebookSize=100,assignment=HARD,pooling=SUM
