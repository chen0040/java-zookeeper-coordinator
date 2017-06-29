# -*- mode: ruby -*# vi: set ft=ruby :
require 'yaml'
vagrantConfig = YAML.load_file 'Vagrantfile.config.yml'
Vagrant.configure("2") do |config|
    config.vm.box = "bento/ubuntu-16.04"

    config.vm.define "zoo1" do |zoo1|
        config.vm.network "private_network", ip: vagrantConfig['ip1'], auto_config: false
    end

    config.vm.define "zoo2" do |zoo2|
        config.vm.network "private_network", ip: vagrantConfig['ip2'], auto_config: false
    end

    config.vm.define "zoo3" do |zoo3|
        config.vm.network "private_network", ip: vagrantConfig['ip3'], auto_config: false
    end

    config.vm.synced_folder "src/", "/home/vagrant/src", owner:"vagrant", group: "vagrant"
    config.vm.synced_folder "devops/", "/home/vagrant/devops", owner:"vagrant", group: "vagrant"

    # VirtualBox specific settings
    config.vm.provider "virtualbox" do |vb|
    vb.gui = false
    vb.memory = "1024"
    vb.cpus = 1
    end

    config.vm.provision "shell", inline: "sudo apt-get update -y"
    config.vm.provision "shell", inline: "sudo apt-get upgrade -y"
    config.vm.provision "shell", inline: "sudo add-apt-repository -y ppa:webupd8team/java"
    config.vm.provision "shell", inline: "sudo apt-get update -y"
    config.vm.provision "shell", inline: "echo 'oracle-java8-installer shared/accepted-oracle-license-v1-1 select true' | sudo debconf-set-selections"
    config.vm.provision "shell", inline: "sudo apt-get install oracle-java8-installer -y"

    config.vm.provision "shell", inline: "yes | cp -rf /home/vagrant/devops/hosts /etc/hosts"

    config.vm.provision "shell", inline: "sudo apt-get install zookeeperd -y"

    config.vm.provision "shell", inline: "sudo /home/vagrant/devops/zk.cfg >> /var/zookeeper/zoo.cfg"

    config.vm.provision "shell", inline: "sudo service zookeeper restart"


end
