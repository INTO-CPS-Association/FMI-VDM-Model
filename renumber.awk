#
# Renumber VDM source files with @OnFail(NNNN, "message", args...) to have contiguous NNNN.
#
# 1. Assumes four digit err numbers.
# 2. Starts from the first number encountered in the file.
#

BEGIN {
    N=0
}
/@OnFail\([0-9]{4},/ {
    if (N == 0)
    {
	match($0, /@OnFail\(([0-9]{4}),/, A)
	# N=int(A[1]/1000)*1000 
	N=A[1]
    }

    print gensub(/@OnFail\([0-9]{4},/, "@OnFail("N",", 1)
    N=N+1
}
!/@OnFail\([0-9]{4},/ {
    print $0
}
END {
}
