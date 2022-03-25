# Generate all XML tests

for x in *.xml
do
    echo "------------ $x"
    n=$(basename $x .xml)

	case $n in
		build_description*)
    		./generateTest.sh fmi3BuildDescription.xsd $x $n
			;;

		*)
			./generateTest.sh fmi3ModelDescription.xsd $x $n
			;;
	esac

    echo Done
done
