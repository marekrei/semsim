#!/bin/sh
#
# A helper script to create expansion terms/hyponyms with many different configurations
#

run(){
	simMeasure=$1
	findHypernyms=$2
	dataset=$3
	model=$4
        
	if $findHypernyms eq "true"
	then
 		type="hypernyms"
	else
		type="hyponyms"
	fi

	echo "Running: $simMeasure $dataset $type $model"
	outputPath="/anfs/bigdisc/mr472/corpora/ParseRerank/expansion/expansion-"$dataset"-"$simMeasure"-"$type"-"$model".txt";
        java3 -Xmx15G -cp ../bin:../lib/*:../../semgraph/bin sem.apps.parsererank.ExpansionFinder $simMeasure $findHypernyms 50 8 /anfs/bigdisc/mr472/semsim_models/$model /anfs/bigdisc/mr472/corpora/ParseRerank/$dataset/lemmas.map > $outputPath
}

model="model3"
#"cosine" "diceGen2" "clarkeDE" "balAPInc" "weightedCosine" "lin" "weedsPrec" "weedsF" "AP" "linD"
for simMeasure in "balAPInc" "weightedCosine" "lin" "weedsPrec" "weedsF" "AP" "linD"
do
	for findHypernyms in "false" "true"
	do
		for dataset in "dev" "test"
		do
			run $simMeasure $findHypernyms $dataset $model
		done
	done
done
