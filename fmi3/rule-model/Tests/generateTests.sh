# Generate all XML tests

for x in *.xml
do
    echo "------------ $x"
    n=$(basename $x .xml)
    ./generateTest.sh $x $n
    echo Done
done
