sysctl -w net.core.somaxconn=2048
sysctl -w net.core.wmem_max=8388608
sysctl -w net.core.rmem_max=33554432
sysctl -w net.core.wmem_default=4194304
sysctl -w net.core.rmem_default=16777216
sysctl -w net.core.dev_weight=4096
sysctl -w net.core.netdev_max_backlog=1000
sysctl -w net.core.rps_sock_flow_entries=0
sysctl -w net.core.netdev_budget=4096
sysctl -w net.ipv4.neigh.default.gc_thresh1=102400
sysctl -w net.ipv4.neigh.default.gc_thresh2=409600
sysctl -w net.ipv4.neigh.default.gc_thresh3=819200

