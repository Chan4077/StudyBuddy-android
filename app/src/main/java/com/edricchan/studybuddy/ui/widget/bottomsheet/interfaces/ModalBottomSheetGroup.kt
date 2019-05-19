package com.edricchan.studybuddy.ui.widget.bottomsheet.interfaces

import com.edricchan.studybuddy.ui.widget.bottomsheet.ModalBottomSheetAdapter
import com.edricchan.studybuddy.ui.widget.bottomsheet.annotations.ModalBottomSheetCheckableBehavior

/**
 * Represents a group of [ModalBottomSheetItem]s
 * @property id The ID of the group so that [ModalBottomSheetItem]s can use the ID
 * @property checkableBehavior The type of the checkable behavior for the group
 * @property onItemCheckedChangeListener A listener which is called when an item's checked state is toggled
 * @property visible Whether the group is visible
 * @property enabled Whether the group is enabled
 * @property selected The selected items
 */
class ModalBottomSheetGroup(
		var id: Int,
		@ModalBottomSheetCheckableBehavior var checkableBehavior: String,
		var onItemCheckedChangeListener: ModalBottomSheetAdapter.OnItemCheckedChangeListener? = null,
		var visible: Boolean = true,
		var enabled: Boolean = true,
		var selected: MutableList<ModalBottomSheetItem> = mutableListOf()
) {
	override fun toString(): String {
		return "ModalBottomSheetGroup(id=$id, checkableBehavior=$checkableBehavior, visible=$visible, enabled=$enabled, selected=[${selected.joinToString()}]"
	}

	companion object {
		/**
		 * Represents that only one item can be checked
		 */
		const val CHECKABLE_BEHAVIOR_NONE = "none"
		/**
		 * Represents that all items can be checked
		 */
		const val CHECKABLE_BEHAVIOR_ALL = "all"
		/**
		 * Represents that only one item can be checked
		 */
		const val CHECKABLE_BEHAVIOR_SINGLE = "single"
	}
}