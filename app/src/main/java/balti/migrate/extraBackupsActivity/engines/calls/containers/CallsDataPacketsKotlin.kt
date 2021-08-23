package balti.migrate.extraBackupsActivity.engines.calls.containers

data class CallsDataPacketsKotlin(val callsCachedFormattedNumber: String?,
                                  val callsCachedLookupUri: String?,
                                  val callsCachedMatchedNumber: String?, val callsCachedName: String?,
                                  val callsCachedNormalizedNumber: String?,
                                  val callsCachedNumberLabel: String?,
                                  val callsCachedNumberType: String?,
                                  val callsCachedPhotoId: String?, val callsCountryIso: String?,
                                  val callsDataUsage: String?, val callsFeatures: String?,
                                  val callsGeocodedLocation: String?, val callsIsRead: String?,
                                  val callsNumber: String?, val callsNumberPresentation: String?,
                                  val callsPhoneAccountComponentName: String?,
                                  val callsPhoneAccountId: String?,
                                  val callsTranscription: String?, val callsType: String?,
                                  val callsVoicemailUri: String?, val callsDate: Long,
                                  val callsDuration: Long, val callsNew: Long, var selected: Boolean)