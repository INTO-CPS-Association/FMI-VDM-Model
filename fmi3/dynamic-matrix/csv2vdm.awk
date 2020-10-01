#
# Script to convert the FMI3 API Matrix into a set of VDM definitions that give the
# kind/state pairs that are permitted for each API.
#
# For example: awk -f csv2vdm.awk FMI3_API_Matrix.csv
#
# See https://docs.google.com/spreadsheets/d/1YsOnQworU2iAynLxWG5TGCQAhOFSl9LS1jE_rDSjfgA
#

BEGIN {
    FS=","
    PROCINFO["sorted_in"]="@ind_str_asc"
    HEADER_LINE=2
    _START=4
    _END=5
    ME_START=7
    ME_END=13
    CS_START=15
    CS_END=22
    SE_START=24
    SE_END=30

    print "/**"
    print " * Values are generated from the FMI3_API_Matrix spreadsheet, using csv2vdm.awk."
    print " * DO NOT EDIT DIRECTLY!"
    print " */"
    print "values"
    print
}
NR==HEADER_LINE {
    for (f=_START; f<=_END; f++)
    {
	if ($f != "")
	{
	    modes[f] = "nil, <" gensub(" ", "_", "g", toupper($f)) ">"
	}
    }

    for (f=ME_START; f<=ME_END; f++)
    {
	if ($f != "")
	{
	    modes[f] = "<ModelExchange>, <" gensub(" ", "_", "g", toupper($f)) ">"
	}
    }

    for (f=CS_START; f<=CS_END; f++)
    {
	if ($f != "")
	{
	    modes[f] = "<CoSimulation>, <" gensub(" ", "_", "g", toupper($f)) ">"
	}
    }

    for (f=SE_START; f<=SE_END; f++)
    {
	if ($f != "")
	{
	    F=gensub("\r", "", "g", $f)
	    modes[f] = "<ScheduledExecution>, <" gensub(" ", "_", "g", toupper(F)) ">"
	}
    }
}
$1 ~ /^fmi3/ {
    api = $1
    delete set

    for (f=_START; f<=_END; f++)
    {
	if ($f != "")
	{
	    set[modes[f]] = 1
	}
    }

    for (f=ME_START; f<=ME_END; f++)
    {
	if ($f != "")
	{
	    set[modes[f]] = 1
	}
    }

    for (f=CS_START; f<=CS_END; f++)
    {
	if ($f != "")
	{
	    set[modes[f]] = 1
	}
    }

    for (f=SE_START; f<=SE_END; f++)
    {
	if ($f != "" && $f != "\r")
	{
	    set[modes[f]] = 1
	}
    }

    print("STATES_" api ": States = {");
    sep = ""

    for (m in set)
    {
    	printf("%s    mk_(%s)", sep, m)
	sep = ",\n"
    }

    print "\n};\n"
}
END {
}
