define puppetdeploy::artifactory(
   $api_key,
   $artifact_url,
   $group_id,
   $artifact_id,
   $version_number,
   $deploy_path
) {
   exec { "deploy_${artifact_id}":
      command => "cmd.exe /c curl -H 'X-JFrog-Art-Api:${api_key}' -o \"${deploy_path}\\${artifact_id}\" \"${artifact_url}/${group_id}/${artifact_id}\"",
	  path => $::path,
      creates => "${deploy_path}\\${artifact_id}",
   }

   file { "${deploy_path}\\${artifact_id}":
      require => Exec["deploy_${artifact_id}"],
   }
}