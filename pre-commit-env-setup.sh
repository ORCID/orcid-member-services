#!/usr/bin/env bash
# exit on errors
set -o errexit -o errtrace -o nounset -o functrace -o pipefail
shopt -s inherit_errexit 2>/dev/null || true
trap 'sk-catch --exit_code $? --line $LINENO --linecallfunc "$BASH_COMMAND" --funcstack $(printf "::%s" ${FUNCNAME[@]}) -o stdout ' ERR

# import shellkit functions
source shellkit_bootstrap.sh

# defaults
current_dir=`pwd`
checkout_name=$(basename `pwd`)
NAME="$(basename "${0}")"
current_dir=`pwd`

#
# functions
#


usage(){
I_USAGE="

  Usage: ${NAME} [OPTIONS]

  Description:
    Pre-commit manages tools but not environments. This will setup some of the environment dependencies using asdf and the .tool-version file in the project

  General usage:

"
  echo "$I_USAGE"
  exit

}

#
# args
#

while :
do
  case ${1-default} in
      --*help|-h         )  usage ; exit 0 ;;
      -v | --verbose )       verbose_arg='-v' VERBOSE=$((VERBOSE+1)); shift ;;
      --) shift ; break ;;
      -*) echo "WARN: Unknown option (ignored): $1" >&2 ; shift ;;
      *)  break ;;
    esac
done

#
# setup build environment from .tool-versions
#

sk-asdf-install-tool-versions
# set JAVA_HOME
. ~/.asdf/plugins/java/set-java-home.bash
_asdf_java_update_java_home

sk-dir-make ~/log

echo "installing pre-commit to run lints"
pre-commit install

echo "installing prettier npms which can be used to debug prettier"
npm install -g prettier@2.8.8 prettier-plugin-java@2.1.0 @prettier/plugin-xml@2.2.0

