import { Pipe, PipeTransform } from '@angular/core'
import { AlertType } from 'src/app/app.constants'

@Pipe({
  name: 'localize',
})
export class LocalizePipe implements PipeTransform {
  transform(value: string, ...args: any[]): any {
    switch (value) {
      case AlertType.SEND_ACTIVATION_SUCCESS:
        return $localize`:@@gatewayApp.msUserServiceMSUser.sendActivate.success.string:Invite sent.`
      case AlertType.SEND_ACTIVATION_FAILURE:
        return $localize`:@@gatewayApp.msUserServiceMSUser.sendActivate.error.string:Invite email couldn't be sent.`
      case AlertType.USER_CREATED:
        return $localize`:@@userServiceApp.user.created.string:User created. Invite sent.`
      case AlertType.USER_UPDATED:
        return $localize`:@@userServiceApp.user.updated.string:User updated successfully`
      case AlertType.USER_DELETED:
        return $localize`:@@userServiceApp.user.deleted.string:User deleted successfully`

      // Affiliation pretty statuses

      case 'User denied access':
        return $localize`:@@gatewayApp.assertionStatus.userDeniedAccess.string:User denied access`
      case 'Pending':
        return $localize`:@@gatewayApp.assertionStatus.pending.string:Pending`
      case 'In ORCID':
        return $localize`:@@gatewayApp.assertionStatus.inOrcid.string:In ORCID`
      case 'User granted access':
        return $localize`:@@gatewayApp.assertionStatus.userGrantedAccess.string:User granted access`
      case 'User deleted from ORCID':
        return $localize`:@@gatewayApp.assertionStatus.userDeletedFromOrcid.string:User deleted from ORCID`
      case 'User revoked access':
        return $localize`:@@gatewayApp.assertionStatus.userRevokedAccess.string:User revoked access`
      case 'Error adding to ORCID':
        return $localize`:@@gatewayApp.assertionStatus.errorAddingToOrcid.string:Error adding to ORCID`
      case 'Error updating in ORCID':
        return $localize`:@@gatewayApp.assertionStatus.errorUpdatingInOrcid.string:Error updating in ORCID`
      case 'Pending retry in ORCID':
        return $localize`:@@gatewayApp.assertionStatus.pendingRetryInOrcid.string:Pending retry in ORCID`
      case 'Error deleting in ORCID':
        return $localize`:@@gatewayApp.assertionStatus.errorDeletingInOrcid.string:Error deleting in ORCID`
      case 'Notification requested':
        return $localize`:@@gatewayApp.assertionStatus.notificationRequested.string:Notification requested`
      case 'Notification sent':
        return $localize`:@@gatewayApp.assertionStatus.notificationSent.string:Notification sent`
      case 'Notification failed':
        return $localize`:@@gatewayApp.assertionStatus.notificationFailed.string:Notification failed`
      case 'Pending update in ORCID':
        return $localize`:@@gatewayApp.assertionStatus.pendingUpdateInOrcid.string:Pending update in ORCID`

      // affiliation types

      case 'EDUCATION':
        return $localize`:@@gatewayApp.AffiliationSection.EDUCATION.string:EDUCATION`
      case 'EMPLOYMENT':
        return $localize`:@@gatewayApp.AffiliationSection.EMPLOYMENT.string:EMPLOYMENT`
      case 'DISTINCTION':
        return $localize`:@@gatewayApp.AffiliationSection.DISTINCTION.string:DISTINCTION`
      case 'INVITED_POSITION':
        return $localize`:@@gatewayApp.AffiliationSection.INVITED_POSITION.string:INVITED_POSITION`
      case 'QUALIFICATION':
        return $localize`:@@gatewayApp.AffiliationSection.QUALIFICATION.string:QUALIFICATION`
      case 'SERVICE':
        return $localize`:@@gatewayApp.AffiliationSection.SERVICE.string:SERVICE`
      case 'MEMBERSHIP':
        return $localize`:@@gatewayApp.AffiliationSection.MEMBERSHIP.string:MEMBERSHIP`

      // months

      case 'January':
        return $localize`:@@gatewayApp.month.january.string:January`
      case 'February':
        return $localize`:@@gatewayApp.month.february.string:February`
      case 'March':
        return $localize`:@@gatewayApp.month.march.string:March`
      case 'April':
        return $localize`:@@gatewayApp.month.april.string:April`
      case 'May':
        return $localize`:@@gatewayApp.month.may.string:May`
      case 'June':
        return $localize`:@@gatewayApp.month.june.string:June`
      case 'July':
        return $localize`:@@gatewayApp.month.july.string:July`
      case 'August':
        return $localize`:@@gatewayApp.month.august.string:August`
      case 'September':
        return $localize`:@@gatewayApp.month.september.string:September`
      case 'October':
        return $localize`:@@gatewayApp.month.october.string:October`
      case 'November':
        return $localize`:@@gatewayApp.month.november.string:November`
      case 'December':
        return $localize`:@@gatewayApp.month.december.string:December`

      // countries

      case 'AD':
        return $localize`:@@country.AD.string:Andorra`
      case 'AE':
        return $localize`:@@country.AE.string:United Arab Emirates`
      case 'AF':
        return $localize`:@@country.AF.string:Afghanistan`
      case 'AG':
        return $localize`:@@country.AG.string:Antigua and Barbuda`
      case 'AI':
        return $localize`:@@country.AI.string:Anguilla`
      case 'AL':
        return $localize`:@@country.AL.string:Albania`
      case 'AM':
        return $localize`:@@country.AM.string:Armenia`
      case 'AO':
        return $localize`:@@country.AO.string:Angola`
      case 'AQ':
        return $localize`:@@country.AQ.string:Antarctica`
      case 'AR':
        return $localize`:@@country.AR.string:Argentina`
      case 'AS':
        return $localize`:@@country.AS.string:American Samoa`
      case 'AT':
        return $localize`:@@country.AT.string:Austria`
      case 'AU':
        return $localize`:@@country.AU.string:Australia`
      case 'AW':
        return $localize`:@@country.AW.string:Aruba`
      case 'AX':
        return $localize`:@@country.AX.string:Åland Islands`
      case 'AZ':
        return $localize`:@@country.AZ.string:Azerbaijan`
      case 'BA':
        return $localize`:@@country.BA.string:Bosnia and Herzegovina`
      case 'BB':
        return $localize`:@@country.BB.string:Barbados`
      case 'BD':
        return $localize`:@@country.BD.string:Bangladesh`
      case 'BE':
        return $localize`:@@country.BE.string:Belgium`
      case 'BF':
        return $localize`:@@country.BF.string:Burkina Faso`
      case 'BG':
        return $localize`:@@country.BG.string:Bulgaria`
      case 'BH':
        return $localize`:@@country.BH.string:Bahrain`
      case 'BI':
        return $localize`:@@country.BI.string:Burundi`
      case 'BJ':
        return $localize`:@@country.BJ.string:Benin`
      case 'BL':
        return $localize`:@@country.BL.string:Saint Barthélemy`
      case 'BM':
        return $localize`:@@country.BM.string:Bermuda`
      case 'BN':
        return $localize`:@@country.BN.string:Brunei`
      case 'BO':
        return $localize`:@@country.BO.string:Bolivia`
      case 'BQ':
        return $localize`:@@country.BQ.string:British Antarctic Territory`
      case 'BR':
        return $localize`:@@country.BR.string:Brazil`
      case 'BS':
        return $localize`:@@country.BS.string:Bahamas`
      case 'BT':
        return $localize`:@@country.BT.string:Bhutan`
      case 'BV':
        return $localize`:@@country.BV.string:Bouvet Island`
      case 'BW':
        return $localize`:@@country.BW.string:Botswana`
      case 'BY':
        return $localize`:@@country.BY.string:Belarus`
      case 'BZ':
        return $localize`:@@country.BZ.string:Belize`
      case 'CA':
        return $localize`:@@country.CA.string:Canada`
      case 'CC':
        return $localize`:@@country.CC.string:Cocos {Keeling} Islands`
      case 'CD':
        return $localize`:@@country.CD.string:Congo - Kinshasa`
      case 'CF':
        return $localize`:@@country.CF.string:Central African Republic`
      case 'CG':
        return $localize`:@@country.CG.string:Congo - Brazzaville`
      case 'CH':
        return $localize`:@@country.CH.string:Switzerland`
      case 'CI':
        return $localize`:@@country.CI.string:Côte d’Ivoire`
      case 'CK':
        return $localize`:@@country.CK.string:Cook Islands`
      case 'CL':
        return $localize`:@@country.CL.string:Chile`
      case 'CM':
        return $localize`:@@country.CM.string:Cameroon`
      case 'CN':
        return $localize`:@@country.CN.string:China`
      case 'CO':
        return $localize`:@@country.CO.string:Colombia`
      case 'CR':
        return $localize`:@@country.CR.string:Costa Rica`
      case 'CU':
        return $localize`:@@country.CU.string:Cuba`
      case 'CV':
        return $localize`:@@country.CV.string:Cape Verde`
      case 'CW':
        return $localize`:@@country.CW.string:Curaçao`
      case 'CX':
        return $localize`:@@country.CX.string:Christmas Island`
      case 'CY':
        return $localize`:@@country.CY.string:Cyprus`
      case 'CZ':
        return $localize`:@@country.CZ.string:Czech Republic`
      case 'DE':
        return $localize`:@@country.DE.string:Germany`
      case 'DJ':
        return $localize`:@@country.DJ.string:Djibouti`
      case 'DK':
        return $localize`:@@country.DK.string:Denmark`
      case 'DM':
        return $localize`:@@country.DM.string:Dominica`
      case 'DO':
        return $localize`:@@country.DO.string:Dominican Republic`
      case 'DZ':
        return $localize`:@@country.DZ.string:Algeria`
      case 'EC':
        return $localize`:@@country.EC.string:Ecuador`
      case 'EE':
        return $localize`:@@country.EE.string:Estonia`
      case 'EG':
        return $localize`:@@country.EG.string:Egypt`
      case 'EH':
        return $localize`:@@country.EH.string:Western Sahara`
      case 'ER':
        return $localize`:@@country.ER.string:Eritrea`
      case 'ES':
        return $localize`:@@country.ES.string:Spain`
      case 'ET':
        return $localize`:@@country.ET.string:Ethiopia`
      case 'FI':
        return $localize`:@@country.FI.string:Finland`
      case 'FJ':
        return $localize`:@@country.FJ.string:Fiji`
      case 'FK':
        return $localize`:@@country.FK.string:Falkland Islands`
      case 'FM':
        return $localize`:@@country.FM.string:Micronesia`
      case 'FO':
        return $localize`:@@country.FO.string:Faroe Islands`
      case 'FR':
        return $localize`:@@country.FR.string:France`
      case 'GA':
        return $localize`:@@country.GA.string:Gabon`
      case 'GB':
        return $localize`:@@country.GB.string:United Kingdom`
      case 'GD':
        return $localize`:@@country.GD.string:Grenada`
      case 'GE':
        return $localize`:@@country.GE.string:Georgia`
      case 'GF':
        return $localize`:@@country.GF.string:French Guiana`
      case 'GG':
        return $localize`:@@country.GG.string:Guernsey`
      case 'GH':
        return $localize`:@@country.GH.string:Ghana`
      case 'GI':
        return $localize`:@@country.GI.string:Gibraltar`
      case 'GL':
        return $localize`:@@country.GL.string:Greenland`
      case 'GM':
        return $localize`:@@country.GM.string:Gambia`
      case 'GN':
        return $localize`:@@country.GN.string:Guinea`
      case 'GP':
        return $localize`:@@country.GP.string:Guadeloupe`
      case 'GQ':
        return $localize`:@@country.GQ.string:Equatorial Guinea`
      case 'GR':
        return $localize`:@@country.GR.string:Greece`
      case 'GS':
        return $localize`:@@country.GS.string:South Georgia and the South Sandwich Islands`
      case 'GT':
        return $localize`:@@country.GT.string:Guatemala`
      case 'GU':
        return $localize`:@@country.GU.string:Guam`
      case 'GW':
        return $localize`:@@country.GW.string:Guinea-Bissau`
      case 'GY':
        return $localize`:@@country.GY.string:Guyana`
      case 'HK':
        return $localize`:@@country.HK.string:Hong Kong SAR China`
      case 'HM':
        return $localize`:@@country.HM.string:Heard Island and McDonald Islands`
      case 'HN':
        return $localize`:@@country.HN.string:Honduras`
      case 'HR':
        return $localize`:@@country.HR.string:Croatia`
      case 'HT':
        return $localize`:@@country.HT.string:Haiti`
      case 'HU':
        return $localize`:@@country.HU.string:Hungary`
      case 'ID':
        return $localize`:@@country.ID.string:Indonesia`
      case 'IE':
        return $localize`:@@country.IE.string:Ireland`
      case 'IL':
        return $localize`:@@country.IL.string:Israel`
      case 'IM':
        return $localize`:@@country.IM.string:Isle of Man`
      case 'IN':
        return $localize`:@@country.IN.string:India`
      case 'IO':
        return $localize`:@@country.IO.string:British Indian Ocean Territory`
      case 'IQ':
        return $localize`:@@country.IQ.string:Iraq`
      case 'IR':
        return $localize`:@@country.IR.string:Iran`
      case 'IS':
        return $localize`:@@country.IS.string:Iceland`
      case 'IT':
        return $localize`:@@country.IT.string:Italy`
      case 'JE':
        return $localize`:@@country.JE.string:Jersey`
      case 'JM':
        return $localize`:@@country.JM.string:Jamaica`
      case 'JO':
        return $localize`:@@country.JO.string:Jordan`
      case 'JP':
        return $localize`:@@country.JP.string:Japan`
      case 'KE':
        return $localize`:@@country.KE.string:Kenya`
      case 'KG':
        return $localize`:@@country.KG.string:Kyrgyzstan`
      case 'KH':
        return $localize`:@@country.KH.string:Cambodia`
      case 'KI':
        return $localize`:@@country.KI.string:Kiribati`
      case 'KM':
        return $localize`:@@country.KM.string:Comoros`
      case 'KN':
        return $localize`:@@country.KN.string:Saint Kitts and Nevis`
      case 'KP':
        return $localize`:@@country.KP.string:North Korea`
      case 'KR':
        return $localize`:@@country.KR.string:South Korea`
      case 'KW':
        return $localize`:@@country.KW.string:Kuwait`
      case 'KY':
        return $localize`:@@country.KY.string:Cayman Islands`
      case 'KZ':
        return $localize`:@@country.KZ.string:Kazakhstan`
      case 'LA':
        return $localize`:@@country.LA.string:Laos`
      case 'LB':
        return $localize`:@@country.LB.string:Lebanon`
      case 'LC':
        return $localize`:@@country.LC.string:Saint Lucia`
      case 'LI':
        return $localize`:@@country.LI.string:Liechtenstein`
      case 'LK':
        return $localize`:@@country.LK.string:Sri Lanka`
      case 'LR':
        return $localize`:@@country.LR.string:Liberia`
      case 'LS':
        return $localize`:@@country.LS.string:Lesotho`
      case 'LT':
        return $localize`:@@country.LT.string:Lithuania`
      case 'LU':
        return $localize`:@@country.LU.string:Luxembourg`
      case 'LV':
        return $localize`:@@country.LV.string:Latvia`
      case 'LY':
        return $localize`:@@country.LY.string:Libya`
      case 'MA':
        return $localize`:@@country.MA.string:Morocco`
      case 'MC':
        return $localize`:@@country.MC.string:Monaco`
      case 'MD':
        return $localize`:@@country.MD.string:Moldova`
      case 'ME':
        return $localize`:@@country.ME.string:Montenegro`
      case 'MF':
        return $localize`:@@country.MF.string:Saint Martin`
      case 'MG':
        return $localize`:@@country.MG.string:Madagascar`
      case 'MH':
        return $localize`:@@country.MH.string:Marshall Islands`
      case 'MK':
        return $localize`:@@country.MK.string:North Macedonia`
      case 'ML':
        return $localize`:@@country.ML.string:Mali`
      case 'MM':
        return $localize`:@@country.MM.string:Myanmar {Burma}`
      case 'MN':
        return $localize`:@@country.MN.string:Mongolia`
      case 'MO':
        return $localize`:@@country.MO.string:Macau SAR China`
      case 'MP':
        return $localize`:@@country.MP.string:Northern Mariana Islands`
      case 'MQ':
        return $localize`:@@country.MQ.string:Martinique`
      case 'MR':
        return $localize`:@@country.MR.string:Mauritania`
      case 'MS':
        return $localize`:@@country.MS.string:Montserrat`
      case 'MT':
        return $localize`:@@country.MT.string:Malta`
      case 'MU':
        return $localize`:@@country.MU.string:Mauritius`
      case 'MV':
        return $localize`:@@country.MV.string:Maldives`
      case 'MW':
        return $localize`:@@country.MW.string:Malawi`
      case 'MX':
        return $localize`:@@country.MX.string:Mexico`
      case 'MY':
        return $localize`:@@country.MY.string:Malaysia`
      case 'MZ':
        return $localize`:@@country.MZ.string:Mozambique`
      case 'NA':
        return $localize`:@@country.NA.string:Namibia`
      case 'NC':
        return $localize`:@@country.NC.string:New Caledonia`
      case 'NE':
        return $localize`:@@country.NE.string:Niger`
      case 'NF':
        return $localize`:@@country.NF.string:Norfolk Island`
      case 'NG':
        return $localize`:@@country.NG.string:Nigeria`
      case 'NI':
        return $localize`:@@country.NI.string:Nicaragua`
      case 'NL':
        return $localize`:@@country.NL.string:Netherlands`
      case 'NO':
        return $localize`:@@country.NO.string:Norway`
      case 'NP':
        return $localize`:@@country.NP.string:Nepal`
      case 'NR':
        return $localize`:@@country.NR.string:Nauru`
      case 'NU':
        return $localize`:@@country.NU.string:Niue`
      case 'NZ':
        return $localize`:@@country.NZ.string:New Zealand`
      case 'OM':
        return $localize`:@@country.OM.string:Oman`
      case 'PA':
        return $localize`:@@country.PA.string:Panama`
      case 'PE':
        return $localize`:@@country.PE.string:Peru`
      case 'PF':
        return $localize`:@@country.PF.string:French Polynesia`
      case 'PG':
        return $localize`:@@country.PG.string:Papua New Guinea`
      case 'PH':
        return $localize`:@@country.PH.string:Philippines`
      case 'PK':
        return $localize`:@@country.PK.string:Pakistan`
      case 'PL':
        return $localize`:@@country.PL.string:Poland`
      case 'PM':
        return $localize`:@@country.PM.string:Saint Pierre and Miquelon`
      case 'PN':
        return $localize`:@@country.PN.string:Pitcairn Islands`
      case 'PR':
        return $localize`:@@country.PR.string:Puerto Rico`
      case 'PS':
        return $localize`:@@country.PS.string:Palestinian Territories`
      case 'PT':
        return $localize`:@@country.PT.string:Portugal`
      case 'PW':
        return $localize`:@@country.PW.string:Palau`
      case 'PY':
        return $localize`:@@country.PY.string:Paraguay`
      case 'QA':
        return $localize`:@@country.QA.string:Qatar`
      case 'RE':
        return $localize`:@@country.RE.string:Réunion`
      case 'RO':
        return $localize`:@@country.RO.string:Romania`
      case 'RS':
        return $localize`:@@country.RS.string:Serbia`
      case 'RU':
        return $localize`:@@country.RU.string:Russia`
      case 'RW':
        return $localize`:@@country.RW.string:Rwanda`
      case 'SA':
        return $localize`:@@country.SA.string:Saudi Arabia`
      case 'SB':
        return $localize`:@@country.SB.string:Solomon Islands`
      case 'SC':
        return $localize`:@@country.SC.string:Seychelles`
      case 'SD':
        return $localize`:@@country.SD.string:Sudan`
      case 'SE':
        return $localize`:@@country.SE.string:Sweden`
      case 'SG':
        return $localize`:@@country.SG.string:Singapore`
      case 'SH':
        return $localize`:@@country.SH.string:Saint Helena`
      case 'SI':
        return $localize`:@@country.SI.string:Slovenia`
      case 'SJ':
        return $localize`:@@country.SJ.string:Svalbard and Jan Mayen`
      case 'SK':
        return $localize`:@@country.SK.string:Slovakia`
      case 'SL':
        return $localize`:@@country.SL.string:Sierra Leone`
      case 'SM':
        return $localize`:@@country.SM.string:San Marino`
      case 'SN':
        return $localize`:@@country.SN.string:Senegal`
      case 'SO':
        return $localize`:@@country.SO.string:Somalia`
      case 'SR':
        return $localize`:@@country.SR.string:Suriname`
      case 'SS':
        return $localize`:@@country.SS.string:South Sudan`
      case 'ST':
        return $localize`:@@country.ST.string:São Tomé and Príncipe`
      case 'SV':
        return $localize`:@@country.SV.string:El Salvador`
      case 'SX':
        return $localize`:@@country.SX.string:Sint Maarten`
      case 'SY':
        return $localize`:@@country.SY.string:Syria`
      case 'SZ':
        return $localize`:@@country.SZ.string:Swaziland`
      case 'TC':
        return $localize`:@@country.TC.string:Turks and Caicos Islands`
      case 'TD':
        return $localize`:@@country.TD.string:Chad`
      case 'TF':
        return $localize`:@@country.TF.string:French Southern Territories`
      case 'TG':
        return $localize`:@@country.TG.string:Togo`
      case 'TH':
        return $localize`:@@country.TH.string:Thailand`
      case 'TJ':
        return $localize`:@@country.TJ.string:Tajikistan`
      case 'TK':
        return $localize`:@@country.TK.string:Tokelau`
      case 'TL':
        return $localize`:@@country.TL.string:Timor-Leste`
      case 'TM':
        return $localize`:@@country.TM.string:Turkmenistan`
      case 'TN':
        return $localize`:@@country.TN.string:Tunisia`
      case 'TO':
        return $localize`:@@country.TO.string:Tonga`
      case 'TR':
        return $localize`:@@country.TR.string:Turkey`
      case 'TT':
        return $localize`:@@country.TT.string:Trinidad and Tobago`
      case 'TV':
        return $localize`:@@country.TV.string:Tuvalu`
      case 'TW':
        return $localize`:@@country.TW.string:Taiwan`
      case 'TZ':
        return $localize`:@@country.TZ.string:Tanzania`
      case 'UA':
        return $localize`:@@country.UA.string:Ukraine`
      case 'UG':
        return $localize`:@@country.UG.string:Uganda`
      case 'UM':
        return $localize`:@@country.UM.string:U.S. Minor Outlying Islands`
      case 'US':
        return $localize`:@@country.US.string:United States`
      case 'UY':
        return $localize`:@@country.UY.string:Uruguay`
      case 'UZ':
        return $localize`:@@country.UZ.string:Uzbekistan`
      case 'VA':
        return $localize`:@@country.VA.string:Vatican City`
      case 'VC':
        return $localize`:@@country.VC.string:Saint Vincent and the Grenadines`
      case 'VE':
        return $localize`:@@country.VE.string:Venezuela`
      case 'VG':
        return $localize`:@@country.VG.string:British Virgin Islands`
      case 'VI':
        return $localize`:@@country.VI.string:U.S. Virgin Islands`
      case 'VN':
        return $localize`:@@country.VN.string:Vietnam`
      case 'VU':
        return $localize`:@@country.VU.string:Vanuatu`
      case 'WF':
        return $localize`:@@country.WF.string:Wallis and Futuna`
      case 'WS':
        return $localize`:@@country.WS.string:Samoa`
      case 'XK':
        return $localize`:@@country.XK.string:Kosovo`
      case 'YE':
        return $localize`:@@country.YE.string:Yemen`
      case 'YT':
        return $localize`:@@country.YT.string:Mayotte`
      case 'ZA':
        return $localize`:@@country.ZA.string:South Africa`
      case 'ZM':
        return $localize`:@@country.ZM.string:Zambia`
      case 'ZW':
        return $localize`:@@country.ZW.string:Zimbabwe`
    }
  }
}
