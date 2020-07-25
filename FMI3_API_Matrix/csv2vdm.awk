BEGIN {
    FS=","
    PROCINFO["sorted_in"]="@ind_str_asc"
    HEADER_LINE=2
    FIRST_FIELD=4
}
NR==HEADER_LINE {
    for (f=FIRST_FIELD; f<NF; f++)
    {
	if ($f != "")
	{
	    modes[f] = "<" gensub(" ", "_", "g", toupper($f)) ">"
	}
    }
}
$1 ~ /^fmi3/ {
    api = $1
    delete set

    for (f=FIRST_FIELD; f<NF; f++)
    {
	if ($f != "")
	{
	    set[modes[f]] = 1
	}
    }

    print("STATES_" api ": States = {");
    sep = ""

    for (m in set)
    {
    	printf("%s    %s", sep, m)
	sep = ",\n"
    }

    print "\n};\n"
}
END {
}
