/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import android.os.Build
import android.os.Parcel
import android.os.ParcelFormatException
import android.os.Parcelable
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import androidx.annotation.RequiresApi

open class ParsedStructureData<Id>(
    val usernameId: Id? = null,
    val passwordId: Id? = null,
    val webDomain: String? = null,
    val packageName: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedStructureData<*>) return false

        if (usernameId != other.usernameId) return false
        if (passwordId != other.passwordId) return false
        if (webDomain != other.webDomain) return false
        if (packageName != other.packageName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = usernameId?.hashCode() ?: 0
        result = 31 * result + (passwordId?.hashCode() ?: 0)
        result = 31 * result + (webDomain?.hashCode() ?: 0)
        result = 31 * result + packageName.hashCode()
        return result
    }
}

// This should be kept in the same order as `allPossibleIds` below.
@RequiresApi(Build.VERSION_CODES.O)
val saveDataTypeMasks = arrayOf(
    SaveInfo.SAVE_DATA_TYPE_USERNAME,
    SaveInfo.SAVE_DATA_TYPE_PASSWORD
)

@RequiresApi(Build.VERSION_CODES.O)
class ParsedStructure(
    usernameId: AutofillId? = null,
    passwordId: AutofillId? = null,
    webDomain: String? = null,
    packageName: String
) : ParsedStructureData<AutofillId>(usernameId, passwordId, webDomain, packageName), Parcelable {

    // This is a paired array with `saveDataTypeMasks` above.
    // We'll use this to calculate both the available autofillIds and the saveInfo mask.
    private val allPossibleIds = arrayOf(usernameId, passwordId)

    val autofillIds: Array<AutofillId> by lazy {
        allPossibleIds.filterNotNull()
            .toTypedArray()
    }

    // Construct the saveInfo mask based upon the autofillIds that are available.
    // This relies on the paired arrays of `saveDataTypeMasks` and the null padded `allPossibleIds`.
    val saveInfoMask: Int by lazy {
        allPossibleIds.mapIndexed { index, autofillId ->
                autofillId?.let { saveDataTypeMasks[index] } ?: 0
            }
            .reduce { totalMask, saveDataTypeMask ->
                totalMask or saveDataTypeMask
            }
    }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(AutofillId::class.java.classLoader),
        parcel.readParcelable(AutofillId::class.java.classLoader),
        parcel.readString(),
        parcel.readString() ?: throw ParcelFormatException("packageName")
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(this.usernameId, flags)
        parcel.writeParcelable(this.passwordId, flags)
        parcel.writeString(this.webDomain)
        parcel.writeString(this.packageName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParsedStructure> {
        override fun createFromParcel(parcel: Parcel): ParsedStructure {
            return ParsedStructure(parcel)
        }

        override fun newArray(size: Int): Array<ParsedStructure?> {
            return arrayOfNulls(size)
        }
    }
}
