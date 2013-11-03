define ['jquery', 'underscore', 'icanhaz'], ($, _, ich) ->

  insertChallengeLine = () ->
    $('form#addTrustPool .challenge .pieces').append(ich.challengeTmpl())

  showTab = (tab) ->
    $(".#{tab}").show()
    $(".#{tab}Tab").addClass('current')

  hideTab = (tab) ->
    $(".#{tab}").hide()
    $(".#{tab}Tab").removeClass('current')

  clear = () ->
    $('input#newKey').val('')
    $('input#newValue').val('')
    $('form#addTrustPool .challenge .pieces').empty()
    $('.pools').show()
    $('.cards').hide()
    $('.requests').hide()
    insertChallengeLine()

  handleTabSwitch = (tab, handler) ->
    $("a.#{tab}Tab").on 'click', (e) ->
      handler()
      e.preventDefault()

  initialize: () ->
    clear()

  onShowPools: (handler) -> handleTabSwitch('pools', handler)
  onShowCards: (handler) -> handleTabSwitch('cards', handler)
  onShowRequests: (handler) -> handleTabSwitch('requests', handler)

  onAddBagItem: (handler) ->
    $('form#addBagItem').on 'submit', (e) ->
      handler($('input#newKey').val(), $('input#newValue').val())
      e.preventDefault()

  onAddChallenge: (handler) ->
    $(document).on 'click', '#addChallenge', (e) ->
      handler()
      e.preventDefault()

  onAddTrustPool: (handler) ->
    $(document).on 'submit', 'form#addTrustPool', (e) ->
      handler($('input#poolName').val(), _.map($('.challengeEntry'), (ce) -> $(ce).val()))
      e.preventDefault()

  onSign: (handler) ->
    $(document).on 'click', 'a.sign', (e) ->
      e.preventDefault()
      handler(e.target.dataset.pool)

  onConnectCancel: (handler) ->
    $(document).on 'click', '.connectionDialog .cancel', (e) ->
      e.preventDefault()
      handler($(e.target).parents('form').children('.poolUri').val())

  onConnectConfirm: (handler) ->
    $(document).on 'click', '.connectionDialog .connect', (e) ->
      e.preventDefault()
      connForm = $(e.target).parents('form')
      handler($('.poolUri', connForm).val(),
              $('.connIdentity', connForm).val(),
              $('.connUri', connForm).val())

  render: (viewMsg) ->
    if _.has(viewMsg, 'wallet')
      $('.wallet').html(ich.walletTmpl(viewMsg.wallet))
      $('.bag .entries').html(ich.bagTmpl(viewMsg.wallet))
    if _.has(viewMsg, 'addChallengeLine')
      insertChallengeLine()
    if _.has(viewMsg, 'addTrustPool')
      $('.pools .entries').append(ich.trustPoolsTmpl(viewMsg.addTrustPool))
    if _.has(viewMsg, 'addCard')
      $('.cards .entries').append(ich.cardTmpl(viewMsg.addCard))
    if _.has(viewMsg, 'displayPoolConnDetails')
      poolUri = viewMsg.displayPoolConnDetails
      $(".dialog[id='#{poolUri}']").html(ich.poolConnectionTmpl(uri: poolUri))
    if _.has(viewMsg, 'removePoolConnDetails')
      $(".dialog[id='#{viewMsg.removePoolConnDetails}']").empty()

    if _.has(viewMsg, 'showTab')
      showTab(viewMsg.showTab)
    if _.has(viewMsg, 'hideTabs')
      _.each(viewMsg.hideTabs, hideTab)
    if _.has(viewMsg, 'reset')
      clear()