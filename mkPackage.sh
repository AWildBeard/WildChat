#!/bin/bash

packageName="wild-chat"
projectName="WildChat"
binaryName="$projectName.jar"
basPkgDir="pkg/"

basdir=$(pwd)
archBaseDir="$(pwd)/"$basPkgDir"pkg-arch/"
archBuildDir=$archBaseDir
archRepoDir=$archBuildDir$packageName"/"
archRepoURL="aur:$packageName.git"
debBaseDir="$(pwd)/"$basPkgDir"pkg-debian/"
debBuildDir=$debBaseDir

echo "Cleaning previous build"
rm -rf $archBaseDir $debBaseDir
echo "Creating arch build dirs"
mkdir -p $archBuildDir
echo "Cloning AUR package"
cd $archBuildDir
git clone $archRepoURL
cd $basdir
echo "Making debian build dirs"
mkdir -p $debBaseDir
cd $debBaseDir
echo "2.0" > debian-binary
mkdir DEBIAN/
echo -e "Package:$packageName \n
		 Version:\n
		 Architecture: all\n
		 Essential: no\n
		 Section: x11\n
		 Priority: optional\n
		 Depends: openjdk-8-jre, openjfx\n
		 Maintainer:\n
		 Installed-Size:\n
		 Description:\n" > DEBIAN/control
touch DEBIAN/md5sums
mkdir usr/
cd $basdir
echo "Making project"
gradle clean build assemble
echo "Copying necessary files to pkg-aur"
cp build/libs/$binaryName $archBuildDir
cp build/libs/$binaryName $debBuildDir
cp $basPkgDir/com.* $archBuildDir
cp $basPkgDir/$packageName* $archBuildDir
cp $basPkgDir/com.* $debBuildDir
cp $basPkgDir/$packageName* $debBuildDir
cd $archBuildDir
tar cvf $packageName.tar com.* $packageName.* $binaryName

exit 0;
