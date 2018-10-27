/*
 * Copyright (C) 2017-2018 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component } from 'react';
import PropTypes from 'prop-types';
import Tooltip from 'components/Tooltip';

import { NAVY } from 'uiTheme/radium/colors';

export const ARROW_OFFSET = 90;

export default class ChartTooltip extends Component {
  static propTypes = {
    position: PropTypes.object,
    content: PropTypes.node
  }
  render() {
    const { content, position } = this.props;
    return (
      <div style={{ position: 'absolute', ...position }}>
        <Tooltip
          id='tooltip'
          type='info'
          placement='top'
          style={styles.tooltip}
          tooltipInnerStyle={styles.tooltipInner}
          tooltipArrowStyle={styles.tooltipArrowStyle}
          content={content}
          arrowOffsetLeft={ARROW_OFFSET}
        />
      </div>
    );
  }
}

const styles = {
  tooltip: {
    pointerEvents: 'none',
    transform: 'translate(-50%, -100%)' // to align tooltip bottom center with provided position
  },
  tooltipInner: {
    background: NAVY,
    color: '#fff',
    boxShadow: '2px 2px 5px 0px rgba(0,0,0,0.05)',
    borderRadius: '2px'
  },
  tooltipArrowStyle: {
    borderTopColor: NAVY
  }
};
