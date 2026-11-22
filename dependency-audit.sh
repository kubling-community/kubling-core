#!/usr/bin/env bash

set -euo pipefail

readonly REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
readonly DEFAULT_REPORT_ROOT="${REPOSITORY_ROOT}/target/dependency-audit"

action="updates"
action_set=false
report_root="${DEFAULT_REPORT_ROOT}"
fail_on_cvss="11"
auto_update=true

usage() {
    cat <<'EOF'
Usage: ./dependency-audit.sh [updates|vulnerabilities|all] [options]

Read-only dependency audit for the complete Maven reactor.

Commands:
  updates          Report newer dependency, property and plugin versions (default).
  vulnerabilities  Generate OWASP Dependency-Check HTML, JSON and SARIF reports.
  all              Run both reports.

Options:
  --output DIR             Report root (default: target/dependency-audit).
  --fail-on-cvss SCORE     Fail the vulnerability scan at SCORE; 11 only reports
                           findings (default: 11).
  --no-update              Use the local OWASP vulnerability database without
                           downloading updates.
  -h, --help               Show this help.

Environment:
  NVD_API_KEY              NVD API key required to refresh the vulnerability
                           database. The value is never passed on the command line;
                           Dependency-Check reads it from the environment.

Each invocation creates a timestamped directory. Maven output is shown in the
terminal and retained as text. Vulnerability reports are written below the
vulnerabilities/ subdirectory.

Version checks compare existing Maven coordinates. They cannot discover that
an artifact moved to a different groupId or artifactId.
EOF
}

die() {
    printf 'dependency-audit: %s\n' "$*" >&2
    exit 2
}

while (($# > 0)); do
    case "$1" in
        updates|vulnerabilities|all)
            if [[ "${action_set}" == true ]]; then
                die "only one command may be selected"
            fi
            action="$1"
            action_set=true
            shift
            ;;
        --output)
            (($# >= 2)) || die "--output requires a directory"
            report_root="$2"
            shift 2
            ;;
        --fail-on-cvss)
            (($# >= 2)) || die "--fail-on-cvss requires a score"
            fail_on_cvss="$2"
            [[ "${fail_on_cvss}" =~ ^([0-9](\.[0-9]+)?|10(\.0+)?|11(\.0+)?)$ ]] \
                || die "--fail-on-cvss must be a number between 0 and 11"
            shift 2
            ;;
        --no-update)
            auto_update=false
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            die "unknown argument: $1"
            ;;
    esac
done

if [[ "${report_root}" != /* ]]; then
    report_root="${REPOSITORY_ROOT}/${report_root}"
fi

readonly timestamp="$(date '+%Y%m%d-%H%M%S')"
readonly report_directory="${report_root}/${timestamp}"
readonly vulnerability_directory="${report_directory}/vulnerabilities"

mkdir -p "${report_directory}"
cd "${REPOSITORY_ROOT}"

read_maven_property() {
    ./mvnw --quiet help:evaluate \
        -Dstyle.color=never \
        -Dexpression="$1" \
        -DforceStdout \
        | tail -n 1
}

readonly versions_plugin_version="$(read_maven_property version.org.codehaus.mojo.versions-maven-plugin)"
readonly dependency_check_version="$(read_maven_property version.org.owasp.dependency-check-maven)"
readonly versions_plugin="org.codehaus.mojo:versions-maven-plugin:${versions_plugin_version}"
readonly dependency_check_plugin="org.owasp:dependency-check-maven:${dependency_check_version}"

readonly -a maven=(
    ./mvnw
    --batch-mode
    --no-transfer-progress
    -DskipTests
    -DskipITs
)

write_run_information() {
    {
        printf 'Generated: %s\n' "$(date --iso-8601=seconds)"
        printf 'Repository: %s\n' "${REPOSITORY_ROOT}"
        printf 'Git branch: %s\n' "$(git branch --show-current)"
        printf 'Git revision: %s\n' "$(git rev-parse HEAD)"
        printf 'Command: %s\n' "${action}"
        printf 'Versions Maven Plugin: %s\n' "${versions_plugin_version}"
        printf 'OWASP Dependency-Check: %s\n' "${dependency_check_version}"
        printf 'Vulnerability database update: %s\n' "${auto_update}"
        printf 'Fail on CVSS: %s\n' "${fail_on_cvss}"
    } > "${report_directory}/run-info.txt"
}

run_and_capture() {
    local report_name="$1"
    shift

    printf '\n==> %s\n' "${report_name}"
    "$@" 2>&1 | tee "${report_directory}/${report_name}.txt"
}

run_updates() {
    run_and_capture property-updates \
        "${maven[@]}" -N \
        -DallowSnapshots=false \
        "${versions_plugin}:display-property-updates"

    run_and_capture managed-dependency-updates \
        "${maven[@]}" -N \
        -DallowSnapshots=false \
        -DprocessDependencyManagement=true \
        "${versions_plugin}:display-dependency-updates"

    run_and_capture module-dependency-updates \
        "${maven[@]}" \
        -DallowSnapshots=false \
        -DprocessDependencyManagement=false \
        "${versions_plugin}:display-dependency-updates"

    run_and_capture plugin-updates \
        "${maven[@]}" -N \
        -DallowSnapshots=false \
        "${versions_plugin}:display-plugin-updates"
}

run_vulnerabilities() {
    local json_report="${vulnerability_directory}/dependency-check-report.json"
    local -a dependency_check_arguments=(
        "${maven[@]}"
        -Dformats=HTML,JSON,SARIF
        "-Dodc.outputDirectory=${vulnerability_directory}"
        "-DfailBuildOnCVSS=${fail_on_cvss}"
        "-DautoUpdate=${auto_update}"
    )

    mkdir -p "${vulnerability_directory}"

    if [[ -n "${NVD_API_KEY:-}" ]]; then
        dependency_check_arguments+=(
            -DnvdApiKeyEnvironmentVariable=NVD_API_KEY
        )
    elif [[ "${auto_update}" == true ]]; then
        die "NVD_API_KEY is required to refresh the OWASP vulnerability database; use --no-update only after a successful refresh"
    fi

    dependency_check_arguments+=("${dependency_check_plugin}:aggregate")
    run_and_capture vulnerability-scan "${dependency_check_arguments[@]}"

    if ! awk '
        /"dataSource"[[:space:]]*:/ { in_data_sources = 1; next }
        in_data_sources && /"name"[[:space:]]*:/ { found_data_source = 1 }
        in_data_sources && /]/ { exit }
        END { exit(found_data_source ? 0 : 1) }
    ' "${json_report}"; then
        printf '%s\n' \
            'dependency-audit: the vulnerability report contains no CVE data sources.' \
            'Run again without --no-update to initialize or refresh the local database.' >&2
        return 1
    fi
}

write_run_information

case "${action}" in
    updates)
        run_updates
        ;;
    vulnerabilities)
        run_vulnerabilities
        ;;
    all)
        run_updates
        run_vulnerabilities
        ;;
esac

printf '\nDependency audit completed. Reports: %s\n' "${report_directory}"
