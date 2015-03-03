test_home=/s/bach/l/under/shaunpa/git/asg2/CS455_ASG2/src

IFS=$'\n'       # make newlines the only separator
set -f          # disable globbing

for i in `cat /s/bach/l/under/shaunpa/git/asg2/CS455_ASG2/src/machine_list`
do
	IFS=- read host url <<< "$i"

        echo 'logging into '$host', using url '$url
	gnome-terminal -x bash -c "ssh -t $host 'cd $test_home; java -cp .:$test_home/jericho-html-3.3.jar cs455.harvester.Crawler 44947 10 $url $test_home/config;bash;'" &
done
