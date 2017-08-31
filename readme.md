
To achieve lineage of data in the Atlas for sample Spark application you should follow:

* `git clone https://github.com/VirtusLab/data_lake_navigation_atlas.git`
* run [HDP sandbox](https://hortonworks.com/products/sandbox/)
* configure [Atlas](https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.0/bk_data-governance/content/ch_hdp_data_governance_install_atlas_ambari.html)
* `sbt run --create`
* log into atlas (default credentials for hdp sandbox are 'admin'/'admin')
* in a search box look for the phrase 'rfc7540'
* choose one of the links and you should get lineage diagram for the source file

