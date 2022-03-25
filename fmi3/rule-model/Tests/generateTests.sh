# Generate all XML tests

for xml in *.xml
do
    echo "------------ $xml"
    name=$(basename $xml .xml)

	case $name in
		build_description*)
    		./generateTest.sh fmi3BuildDescription.xsd $xml $name
			;;

		*)
			./generateTest.sh fmi3ModelDescription.xsd $xml $name
			;;
	esac

    echo Done
done
