class puppetdeploy(
   $in_api_key,
   $in_artifact_url,
   $in_group_id,
   $in_artifact_id,
   $in_version_number,
   $in_base_deploy_path
) {
   # Create deployment directory tree
   $deployment_dir = "${in_base_deploy_path}\\${in_artifact_id}\\${in_version_number}"
   file { [ "${in_base_deploy_path}",
            "${in_base_deploy_path}\\${in_artifact_id}",
			"${deployment_dir}"]:
      ensure => 'directory',
   }

   # Use Artifactory module to deploy
   puppetdeploy::artifactory { "deploy_${in_artifact_id}": 
      api_key        => $in_api_key,
      artifact_url   => $in_artifact_url,
      group_id       => $in_group_id,
      artifact_id    => $in_artifact_id,
      version_number => $in_version_number,
	  deploy_path    => "${deployment_dir}"
   }
}